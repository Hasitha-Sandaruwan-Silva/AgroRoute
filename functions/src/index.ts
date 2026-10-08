import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import { createHash } from "crypto";
import {
  NotificationEventType,
  NotificationResource,
  resolveNotificationRecipient,
  routeForNotification,
} from "./notificationPolicy";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

const db = admin.firestore();
const fcm = admin.messaging();

/**
 * Trusted backend for AgroRoute push notifications.
 *
 * The Android app only calls these endpoints with its authenticated Firebase ID
 * token; it never holds privileged Firebase Admin credentials. Device tokens are
 * stored under the authenticated UID, and sender identity is verified by Firebase Auth.
 */

/** Stores (or replaces) the FCM token for a user. Served only by the app's onCall. */
export const registerDeviceToken = functions.https.onCall(
  async (data, context) => {
    const userId = context.auth?.uid;
    if (!userId) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Only authenticated users can register a device token."
      );
    }

    const { token } = data as { token?: string };

    if (typeof token !== "string" || token.trim().length === 0 || token.length > 4096) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "A non-empty device token is required."
      );
    }

    const tokenKey = createHash("sha256").update(token).digest("hex");
    const deviceTokenRef = db.collection("device_tokens").doc(userId).collection("devices").doc(tokenKey);
    const ownerRef = db.collection("device_token_owners").doc(tokenKey);
    await db.runTransaction(async (transaction) => {
      const owner = await transaction.get(ownerRef);
      const previousUserId = stringField(owner.data(), "userId");
      if (previousUserId && previousUserId !== userId) {
        transaction.delete(
          db.collection("device_tokens").doc(previousUserId).collection("devices").doc(tokenKey)
        );
      }
      transaction.set(ownerRef, {
        userId,
        token,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      transaction.set(deviceTokenRef, {
        userId,
        token,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    });

    return { ok: true, userId };
  }
);

/** Removes a stale token (e.g. on sign-out or unregistration). */
export const unregisterDeviceToken = functions.https.onCall(
  async (data, context) => {
    const auth = context.auth;
    if (!auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Only authenticated users can unregister a device token."
      );
    }

    const userId = context.auth?.uid;
    if (!userId) {
      throw new functions.https.HttpsError("unauthenticated", "Only authenticated users can unregister a device token.");
    }
    const requestedUserId = (data as { userId?: string }).userId;
    if (requestedUserId && requestedUserId !== userId) {
      throw new functions.https.HttpsError("permission-denied", "A user can only remove their own device token.");
    }
    const token = (data as { token?: string }).token;
    if (typeof token !== "string" || !token.trim()) {
      throw new functions.https.HttpsError("invalid-argument", "The current device token is required.");
    }
    const tokenKey = createHash("sha256").update(token).digest("hex");
    const deviceTokenRef = db.collection("device_tokens").doc(userId).collection("devices").doc(tokenKey);
    const ownerRef = db.collection("device_token_owners").doc(tokenKey);
    await db.runTransaction(async (transaction) => {
      const [device, owner] = await Promise.all([
        transaction.get(deviceTokenRef),
        transaction.get(ownerRef),
      ]);
      if (device.get("token") === token && owner.get("userId") === userId) {
        transaction.delete(deviceTokenRef);
        transaction.delete(ownerRef);
      }
    });

    return { ok: true, userId };
  }
);

interface SendNotificationRequest {
  recipientUserId?: string;
  actorUserId?: string;
  eventType: string;
  payload: Record<string, unknown>;
  notification?: Record<string, unknown>;
}

type DocumentData = FirebaseFirestore.DocumentData;

function stringField(data: DocumentData | undefined, field: string): string {
  const value = data?.[field];
  return typeof value === "string" ? value : "";
}

function numberField(data: DocumentData | undefined, field: string): number | undefined {
  const value = data?.[field];
  return typeof value === "number" ? value : undefined;
}

function objectData(value: unknown): Record<string, unknown> {
  if (value === null || typeof value !== "object" || Array.isArray(value)) return {};
  return value as Record<string, unknown>;
}

function requiredId(payload: Record<string, unknown>, field: string): string {
  const value = payload[field];
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new functions.https.HttpsError("invalid-argument", `${field} is required.`);
  }
  return value;
}

function messageFor(
  eventType: NotificationEventType,
  cropName: string,
  supplierName: string,
  reason: string
): { title: string; body: string } {
  const crop = cropName || "your harvest";
  const supplier = supplierName || "A supplier";
  switch (eventType) {
    case "ORDER_REQUEST_CREATED": return { title: "New order request", body: `${supplier} requested ${crop}. Review it in your requests.` };
    case "ORDER_REQUEST_ACCEPTED": return { title: "Order request accepted", body: `Your request for ${crop} was accepted. Your order is awaiting pickup.` };
    case "ORDER_REQUEST_REJECTED": return { title: "Order request rejected", body: `Your request for ${crop} was rejected.` };
    case "ORDER_REQUEST_CANCELLED": return { title: "Order request cancelled", body: `${supplier} cancelled their request for ${crop}.` };
    case "BID_CREATED": return { title: "New bid", body: `${supplier} placed a bid for ${crop}.` };
    case "BID_ACCEPTED": return { title: "Bid accepted", body: `Your bid for ${crop} was accepted. Your order is awaiting pickup.` };
    case "BID_REJECTED": return { title: "Bid rejected", body: `Your bid for ${crop} was rejected.` };
    case "BID_WITHDRAWN": return { title: "Bid withdrawn", body: `${supplier} withdrew a bid for ${crop}.` };
    case "ORDER_IN_TRANSIT": return { title: "Order in transit", body: `Your order for ${crop} is now in transit.` };
    case "ORDER_CANCELLED": return { title: "Order cancelled", body: `Your order for ${crop} was cancelled.` };
    case "ORDER_COMPLETED": return { title: "Order completed", body: `Your order for ${crop} has been delivered.` };
    case "HARVEST_READY": return { title: "Harvest ready", body: `${crop} is ready. Review your order request in Orders.` };
    case "HARVEST_FAILED": return { title: "Harvest unavailable", body: `The expected harvest of ${crop} is no longer available.` };
    case "HARVEST_UPDATE": return { title: "Harvest estimate updated", body: `The expected harvest details for ${crop} have changed.` };
  }
}

/**
 * Resolves an event against persisted business records before delivering data-only FCM.
 * The authenticated actor, recipient, route and idempotency key are never trusted from
 * client-provided display fields.
 */
export const sendNotification = functions.https.onCall(
  async (data, context) => {
    const actorId = context.auth?.uid;
    if (!actorId) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Only authenticated users may trigger push delivery."
      );
    }

    const req = data as SendNotificationRequest;
    if (req.actorUserId !== actorId || !req.recipientUserId) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "The authenticated actor must match the event and a recipient is required."
      );
    }

    const supportedEvents: NotificationEventType[] = [
      "ORDER_REQUEST_CREATED", "ORDER_REQUEST_ACCEPTED", "ORDER_REQUEST_REJECTED", "ORDER_REQUEST_CANCELLED",
      "BID_CREATED", "BID_ACCEPTED", "BID_REJECTED", "BID_WITHDRAWN",
      "ORDER_IN_TRANSIT", "ORDER_CANCELLED", "ORDER_COMPLETED", "HARVEST_READY", "HARVEST_FAILED", "HARVEST_UPDATE",
    ];
    if (!supportedEvents.includes(req.eventType as NotificationEventType)) {
      throw new functions.https.HttpsError("invalid-argument", "Unsupported notification event.");
    }
    const eventType = req.eventType as NotificationEventType;
    const actorProfile = await db.collection("users").doc(actorId).get();
    const actorRole = stringField(actorProfile.data(), "role");
    const expectedActorRole = eventType.startsWith("ORDER_REQUEST_")
      ? eventType === "ORDER_REQUEST_CREATED" || eventType === "ORDER_REQUEST_CANCELLED" ? "SUPPLIER" : "FARMER"
      : eventType.startsWith("BID_")
        ? eventType === "BID_CREATED" || eventType === "BID_WITHDRAWN" ? "SUPPLIER" : "FARMER"
        : eventType === "ORDER_COMPLETED" ? "SUPPLIER"
          : eventType.startsWith("ORDER_") ? "FARMER"
            : "FARMER";
    if (!actorProfile.exists || (eventType !== "ORDER_CANCELLED" && actorRole !== expectedActorRole) ||
        (eventType === "ORDER_CANCELLED" && actorRole !== "FARMER" && actorRole !== "SUPPLIER")) {
      throw new functions.https.HttpsError("permission-denied", "The authenticated account role cannot perform this notification event.");
    }
    const payload = objectData(req.payload);
    const resource: NotificationResource = {};
    let entityId = "";
    let cropName = "";
    let supplierName = "";
    let orderId = "";
    let bidId = "";
    let requestId = "";
    let listingId = "";
    let reason = "";

    if (eventType.startsWith("ORDER_REQUEST_")) {
      requestId = requiredId(payload, "request_id");
      const requestSnap = await db.collection("order_requests").doc(requestId).get();
      const request = requestSnap.data();
      if (!requestSnap.exists || !request) throw new functions.https.HttpsError("not-found", "Order request not found.");
      resource.request = {
        requestId,
        farmerId: stringField(request, "farmerId"),
        supplierId: stringField(request, "supplierId"),
        status: stringField(request, "status"),
        updatedAt: numberField(request, "updatedAt"),
      };
      cropName = stringField(request, "cropName");
      supplierName = stringField(request, "supplierName");
      listingId = stringField(request, "listingId");
      entityId = requestId;
      reason = stringField(request, "harvestFailureReason");
      if (eventType === "ORDER_REQUEST_ACCEPTED") {
        const orderSnap = await db.collection("orders").doc(requestId).get();
        const order = orderSnap.data();
        if (!orderSnap.exists || !order || stringField(order, "farmerId") !== resource.request.farmerId ||
            stringField(order, "supplierId") !== resource.request.supplierId) {
          throw new functions.https.HttpsError("failed-precondition", "Accepted request has no matching order.");
        }
        orderId = requestId;
      }
    } else if (eventType.startsWith("BID_")) {
      if (eventType === "BID_ACCEPTED") {
        orderId = requiredId(payload, "order_id");
        const orderSnap = await db.collection("orders").doc(orderId).get();
        const order = orderSnap.data();
        if (!orderSnap.exists || !order) throw new functions.https.HttpsError("not-found", "Order not found.");
        const bidKey = stringField(order, "bidId");
        const bidSnap = await db.collection("bids").doc(bidKey).get();
        const bid = bidSnap.data();
        if (!bidSnap.exists || !bid) throw new functions.https.HttpsError("not-found", "Accepted bid not found.");
        resource.order = {
          orderId,
          farmerId: stringField(order, "farmerId"),
          supplierId: stringField(order, "supplierId"),
          status: stringField(order, "status"),
          updatedAt: numberField(order, "updatedAt"),
        };
        resource.bid = {
          bidId: bidKey,
          supplierId: stringField(bid, "supplierId"),
          status: stringField(bid, "status"),
          updatedAt: numberField(bid, "updatedAt"),
        };
        cropName = stringField(order, "cropName");
        listingId = stringField(order, "listingId");
        bidId = bidKey;
        entityId = orderId;
      } else {
        bidId = requiredId(payload, "bid_id");
        const bidSnap = await db.collection("bids").doc(bidId).get();
        const bid = bidSnap.data();
        if (!bidSnap.exists || !bid) throw new functions.https.HttpsError("not-found", "Bid not found.");
        listingId = stringField(bid, "listingId");
        const listingSnap = await db.collection("listings").doc(listingId).get();
        const listing = listingSnap.data();
        if (!listingSnap.exists || !listing) throw new functions.https.HttpsError("not-found", "Listing not found.");
        resource.bid = {
          bidId,
          supplierId: stringField(bid, "supplierId"),
          status: stringField(bid, "status"),
          updatedAt: numberField(bid, "updatedAt"),
        };
        resource.listing = {
          listingId,
          farmerId: stringField(listing, "farmerId"),
          harvestStatus: stringField(listing, "harvestStatus"),
          harvestUpdatedAt: numberField(listing, "harvestUpdatedAt"),
        };
        cropName = stringField(listing, "cropName");
        supplierName = stringField(bid, "supplierName");
        entityId = bidId;
      }
    } else if (eventType.startsWith("ORDER_")) {
      orderId = requiredId(payload, "order_id");
      const orderSnap = await db.collection("orders").doc(orderId).get();
      const order = orderSnap.data();
      if (!orderSnap.exists || !order) throw new functions.https.HttpsError("not-found", "Order not found.");
      resource.order = {
        orderId,
        farmerId: stringField(order, "farmerId"),
        supplierId: stringField(order, "supplierId"),
        status: stringField(order, "status"),
        updatedAt: numberField(order, "updatedAt"),
      };
      cropName = stringField(order, "cropName");
      listingId = stringField(order, "listingId");
      entityId = orderId;
    } else {
      listingId = requiredId(payload, "listing_id");
      requestId = requiredId(payload, "request_id");
      const [listingSnap, requestSnap] = await Promise.all([
        db.collection("listings").doc(listingId).get(),
        db.collection("order_requests").doc(requestId).get(),
      ]);
      const listing = listingSnap.data();
      const request = requestSnap.data();
      if (!listingSnap.exists || !listing || !requestSnap.exists || !request ||
          stringField(request, "listingId") !== listingId) {
        throw new functions.https.HttpsError("not-found", "Harvest or related order request not found.");
      }
      resource.listing = {
        listingId,
        farmerId: stringField(listing, "farmerId"),
        harvestStatus: stringField(listing, "harvestStatus"),
        harvestUpdatedAt: numberField(listing, "harvestUpdatedAt"),
      };
      resource.relatedRequest = {
        requestId,
        supplierId: stringField(request, "supplierId"),
        status: stringField(request, "status"),
        isPreHarvest: request.isPreHarvest === true,
      };
      cropName = stringField(listing, "cropName");
      supplierName = stringField(request, "supplierName");
      reason = stringField(listing, "harvestFailureReason");
      entityId = listingId;
    }

    const resolved = resolveNotificationRecipient(eventType, actorId, resource);
    if (!resolved || resolved.userId !== req.recipientUserId) {
      throw new functions.https.HttpsError("permission-denied", "This account is not the recipient of that event.");
    }
    const route = routeForNotification(eventType, resolved.role, { orderId, listingId });
    if (!route) throw new functions.https.HttpsError("failed-precondition", "No destination exists for this notification.");

    const eventId = createHash("sha256")
      .update(`${eventType}:${resolved.entityId}:${resolved.version}:${resolved.userId}`)
      .digest("hex");
    const eventRef = db.collection("notification_events").doc(eventId);
    await db.runTransaction(async (transaction) => {
      const previous = await transaction.get(eventRef);
      if (previous.exists) return;
      transaction.create(eventRef, {
        eventType,
        actorId,
        recipientUserId: resolved.userId,
        entityId: resolved.entityId,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    });

    const recipientRef = db.collection("device_tokens").doc(resolved.userId);
    const tokenSnapshots = await recipientRef.collection("devices").get();
    const tokens = tokenSnapshots.docs
      .map((document) => ({ ref: document.ref, token: stringField(document.data(), "token") }))
      .filter((device) => device.token.length > 0);
    if (tokens.length === 0) {
      await eventRef.delete();
      return { ok: true, sent: false, reason: "NO_TOKEN", eventId };
    }
    const pendingTokens = (await Promise.all(tokens.map(async (device) => {
      const delivery = await eventRef.collection("deliveries").doc(device.ref.id).get();
      return delivery.exists ? null : device;
    }))).filter((device): device is { ref: FirebaseFirestore.DocumentReference; token: string } => device !== null);
    if (pendingTokens.length === 0) return { ok: true, sent: false, reason: "DUPLICATE", eventId };

    const { title, body } = messageFor(eventType, cropName, supplierName, reason);
    const messageData: Record<string, string> = {
      event_type: eventType,
      event_id: eventId,
      recipient_user_id: resolved.userId,
      title,
      body,
      route,
    };
    const stringPayload: Record<string, string> = {
      crop_name: cropName,
      supplier_name: supplierName,
      reason,
      order_id: orderId,
      bid_id: bidId,
      request_id: requestId,
      listing_id: listingId,
    };
    Object.entries(stringPayload).forEach(([key, value]) => {
      if (value) messageData[key] = value;
    });

    try {
      const results = await Promise.allSettled(pendingTokens.map(async (device) => {
        const deliveryRef = eventRef.collection("deliveries").doc(device.ref.id);
        const claimed = await db.runTransaction(async (transaction) => {
          const existing = await transaction.get(deliveryRef);
          if (existing.exists) {
            const claimedAt = existing.get("claimedAt") as FirebaseFirestore.Timestamp | undefined;
            const claimExpired = existing.get("status") === "SENDING" &&
              claimedAt !== undefined && Date.now() - claimedAt.toMillis() > 120_000;
            if (!claimExpired) return false;
            transaction.update(deliveryRef, {
              status: "SENDING",
              claimedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
            return true;
          }
          transaction.create(deliveryRef, {
            status: "SENDING",
            claimedAt: admin.firestore.FieldValue.serverTimestamp(),
          });
          return true;
        });
        if (!claimed) return;
        try {
          const messageId = await fcm.send({
            token: device.token,
            data: messageData,
            android: { priority: "high" },
          });
          await deliveryRef.set({
            status: "SENT",
            sentAt: admin.firestore.FieldValue.serverTimestamp(),
            messageId,
          });
        } catch (error) {
          const code = (error as { code?: string }).code ?? "";
          if (["messaging/invalid-registration-token", "messaging/registration-token-not-registered"].includes(code)) {
            const ownerRef = db.collection("device_token_owners").doc(device.ref.id);
            await db.runTransaction(async (transaction) => {
              const [current, owner] = await Promise.all([
                transaction.get(device.ref),
                transaction.get(ownerRef),
              ]);
              if (current.get("token") === device.token) transaction.delete(device.ref);
              if (owner.get("token") === device.token) transaction.delete(ownerRef);
            });
            await deliveryRef.set({ status: "INVALID", updatedAt: admin.firestore.FieldValue.serverTimestamp() });
            return;
          }
          await deliveryRef.delete();
          throw error;
        }
      }));
      const failed = results.filter((result) => result.status === "rejected");
      if (failed.length > 0) throw new Error(`${failed.length} device delivery/deliveries failed`);
      await eventRef.update({ sentAt: admin.firestore.FieldValue.serverTimestamp() });
      return { ok: true, sent: true, eventId };
    } catch (error) {
      console.error("FCM send failed", error);
      throw new functions.https.HttpsError("internal", "Failed to deliver push to the device.");
    }
  }
);

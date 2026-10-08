export type NotificationEventType =
  | "ORDER_REQUEST_CREATED"
  | "ORDER_REQUEST_ACCEPTED"
  | "ORDER_REQUEST_REJECTED"
  | "ORDER_REQUEST_CANCELLED"
  | "BID_CREATED"
  | "BID_ACCEPTED"
  | "BID_REJECTED"
  | "BID_WITHDRAWN"
  | "ORDER_IN_TRANSIT"
  | "ORDER_CANCELLED"
  | "ORDER_COMPLETED"
  | "HARVEST_READY"
  | "HARVEST_FAILED"
  | "HARVEST_UPDATE";

export interface NotificationResource {
  request?: { requestId: string; farmerId: string; supplierId: string; status: string; updatedAt?: number };
  bid?: { bidId: string; supplierId: string; status: string; updatedAt?: number };
  order?: { orderId: string; farmerId: string; supplierId: string; status: string; updatedAt?: number };
  listing?: { listingId: string; farmerId: string; harvestStatus: string; harvestUpdatedAt?: number };
  relatedRequest?: { requestId: string; supplierId: string; status: string; isPreHarvest: boolean };
}

export interface NotificationRecipient {
  userId: string;
  role: "FARMER" | "SUPPLIER";
  entityId: string;
  version: string;
}

export function resolveNotificationRecipient(
  eventType: NotificationEventType,
  actorId: string,
  resource: NotificationResource
): NotificationRecipient | null {
  let userId: string | undefined;
  let role: NotificationRecipient["role"];
  let entityId: string | undefined;
  let version: number | undefined;

  switch (eventType) {
    case "ORDER_REQUEST_CREATED":
    case "ORDER_REQUEST_ACCEPTED":
    case "ORDER_REQUEST_REJECTED":
    case "ORDER_REQUEST_CANCELLED": {
      const request = resource.request;
      const expected = {
        ORDER_REQUEST_CREATED: ["PENDING", request?.supplierId, request?.farmerId, "FARMER"],
        ORDER_REQUEST_ACCEPTED: ["ACCEPTED", request?.farmerId, request?.supplierId, "SUPPLIER"],
        ORDER_REQUEST_REJECTED: ["REJECTED", request?.farmerId, request?.supplierId, "SUPPLIER"],
        ORDER_REQUEST_CANCELLED: ["CANCELLED", request?.supplierId, request?.farmerId, "FARMER"]
      }[eventType];
      if (!request || request.status !== expected[0] || actorId !== expected[1]) return null;
      userId = expected[2];
      role = expected[3] as NotificationRecipient["role"];
      entityId = request.requestId;
      version = request.updatedAt;
      break;
    }
    case "BID_CREATED":
    case "BID_REJECTED":
    case "BID_WITHDRAWN": {
      const { bid, listing } = resource;
      if (!bid || !listing) return null;
      const expected = {
        BID_CREATED: ["PENDING", bid.supplierId, listing.farmerId, "FARMER"],
        BID_REJECTED: ["REJECTED", listing.farmerId, bid.supplierId, "SUPPLIER"],
        BID_WITHDRAWN: ["WITHDRAWN", bid.supplierId, listing.farmerId, "FARMER"]
      }[eventType];
      if (bid.status !== expected[0] || actorId !== expected[1]) return null;
      userId = expected[2];
      role = expected[3] as NotificationRecipient["role"];
      entityId = bid.bidId;
      version = bid.updatedAt;
      break;
    }
    case "BID_ACCEPTED": {
      const { order, bid } = resource;
      if (!order || !bid || order.status !== "AWAITING_PICKUP" || bid.status !== "ACCEPTED" ||
          actorId !== order.farmerId || bid.supplierId !== order.supplierId) return null;
      userId = order.supplierId;
      role = "SUPPLIER";
      entityId = order.orderId;
      version = order.updatedAt;
      break;
    }
    case "ORDER_IN_TRANSIT":
    case "ORDER_CANCELLED":
    case "ORDER_COMPLETED": {
      const order = resource.order;
      const expectedStatus = {
        ORDER_IN_TRANSIT: "IN_TRANSIT",
        ORDER_CANCELLED: "CANCELLED",
        ORDER_COMPLETED: "DELIVERED"
      }[eventType];
      if (!order || order.status !== expectedStatus) return null;
      if (eventType === "ORDER_IN_TRANSIT" && actorId !== order.farmerId) return null;
      if (eventType === "ORDER_COMPLETED" && actorId !== order.supplierId) return null;
      if (eventType === "ORDER_CANCELLED" && actorId !== order.farmerId && actorId !== order.supplierId) return null;
      userId = actorId === order.farmerId ? order.supplierId : order.farmerId;
      role = userId === order.farmerId ? "FARMER" : "SUPPLIER";
      entityId = order.orderId;
      version = order.updatedAt;
      break;
    }
    case "HARVEST_READY":
    case "HARVEST_FAILED":
    case "HARVEST_UPDATE": {
      const { listing, relatedRequest } = resource;
      if (!listing || !relatedRequest ||
          !relatedRequest.isPreHarvest || actorId !== listing.farmerId) return null;
      const expectedStatus = eventType === "HARVEST_READY" ? "READY" :
        eventType === "HARVEST_FAILED" ? "FAILED" : "EXPECTED";
      const expectedRequestStatus = eventType === "HARVEST_FAILED" ? "CANCELLED" : "PENDING";
      if (relatedRequest.status !== expectedRequestStatus) return null;
      if (listing.harvestStatus !== expectedStatus) return null;
      userId = relatedRequest.supplierId;
      role = "SUPPLIER";
      entityId = `${listing.listingId}:${relatedRequest.requestId}`;
      version = listing.harvestUpdatedAt;
      break;
    }
  }

  if (!userId || !entityId || userId === actorId) return null;
  return { userId, role, entityId, version: String(version ?? "") };
}

export function routeForNotification(
  eventType: NotificationEventType,
  role: NotificationRecipient["role"],
  ids: { orderId?: string; listingId?: string }
): string | null {
  switch (eventType) {
    case "ORDER_REQUEST_CREATED": return role === "FARMER" ? "farmer_order_requests" : null;
    case "ORDER_REQUEST_ACCEPTED": return role === "SUPPLIER" && ids.orderId ? `supplier_order_detail/${ids.orderId}` : null;
    case "ORDER_REQUEST_REJECTED": return role === "SUPPLIER" ? "supplier_orders" : null;
    case "ORDER_REQUEST_CANCELLED": return role === "FARMER" ? "farmer_order_requests" : null;
    case "BID_CREATED":
    case "BID_WITHDRAWN": return role === "FARMER" && ids.listingId ? `view_bids/${ids.listingId}` : null;
    case "BID_REJECTED": return role === "SUPPLIER" ? "supplier_bids" : null;
    case "BID_ACCEPTED":
    case "ORDER_COMPLETED": return role === "SUPPLIER" && ids.orderId
      ? `supplier_order_detail/${ids.orderId}`
      : eventType === "ORDER_COMPLETED" && role === "FARMER" && ids.orderId
        ? `farmer_order_detail/${ids.orderId}`
        : null;
    case "ORDER_IN_TRANSIT":
    case "ORDER_CANCELLED": return ids.orderId
      ? role === "FARMER"
        ? `farmer_order_detail/${ids.orderId}`
        : `supplier_order_detail/${ids.orderId}`
      : null;
    case "HARVEST_READY":
    case "HARVEST_FAILED":
    case "HARVEST_UPDATE": return role === "SUPPLIER" ? "supplier_orders" : null;
  }
}

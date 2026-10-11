import { randomInt, timingSafeEqual } from "crypto";

// Reuses the existing one-point order reward, now earned independently at pickup.
export const ORDER_PICKUP_POINTS = 1;
const CODE_LIFETIME_MS = 24 * 60 * 60 * 1000;
const ATTEMPT_WINDOW_MS = 30 * 60 * 1000;
const MAX_ATTEMPTS = 5;

/** Secrets and reward receipts are Admin-only; the Farmer never receives the code. */
export async function orderPickupTransaction(
  db: FirebaseFirestore.Firestore, orderId: string, userId: string,
  action: "view" | "verify" | "collect", submittedCode?: string,
) {
  const orderRef = db.doc(`orders/${orderId}`);
  const verificationRef = db.doc(`order_pickup_verifications/${orderId}`);
  return db.runTransaction(async tx => {
    const order = (await tx.get(orderRef)).data();
    if (!order || order.orderId !== orderId) throw new Error("Order not found");
    const isFarmer = userId === order.farmerId;
    const isSupplier = userId === order.supplierId;
    if (!isFarmer && !isSupplier) throw new Error("Only an order participant can access pickup verification");
    const userRef = db.doc(`users/${userId}`);
    const [userSnapshot, verificationSnapshot, completionSnapshot] = await Promise.all([
      tx.get(userRef), tx.get(verificationRef), tx.get(db.doc(`order_completion_records/${orderId}`)),
    ]);
    const user = userSnapshot.data();
    if (user?.userId !== userId || user?.role !== (isFarmer ? "FARMER" : "SUPPLIER")) {
      throw new Error("Order participant profile does not match");
    }
    if (action === "verify" && !isFarmer) throw new Error("Only the Farmer can verify handover");
    if (action === "collect" && !isSupplier) throw new Error("Only the Supplier can confirm pickup");
    let verification = verificationSnapshot.data();
    const now = Date.now();
    const response = (value: FirebaseFirestore.DocumentData | undefined) => ({
      farmerConfirmed: value?.farmerConfirmed === true,
      supplierConfirmed: value?.supplierConfirmed === true,
      legacy: !value && order.status !== "AWAITING_PICKUP",
      code: isSupplier && !value?.farmerConfirmed && value?.expiresAt > now &&
        ["AWAITING_PICKUP", "IN_TRANSIT", "DELIVERED"].includes(order.status) ? value?.code ?? null : null,
      expiresAt: isSupplier ? value?.expiresAt ?? null : null,
    });
    if (action === "view") {
      // Opening historical orders never generates a reward or enrolls completed pickups.
      if (isSupplier && (order.status === "AWAITING_PICKUP" || verification?.supplierConfirmed &&
          ["IN_TRANSIT", "DELIVERED"].includes(order.status)) &&
          (!completionSnapshot.exists || verification?.supplierConfirmed) &&
          !verification?.farmerConfirmed && (!verification || verification.expiresAt <= now)) {
        verification = { ...(verification ?? {}), code: randomInt(0, 1_000_000).toString().padStart(6, "0"),
          expiresAt: now + CODE_LIFETIME_MS, farmerConfirmed: false,
          supplierConfirmed: verification?.supplierConfirmed === true };
        tx.set(verificationRef, verification);
      }
      return response(verification);
    }
    if (!verification || completionSnapshot.exists && !verification.supplierConfirmed) {
      throw new Error("Pickup verification unavailable for this legacy order");
    }
    if (order.status === "CANCELLED") throw new Error("Cancelled orders cannot be confirmed");
    const flag = action === "verify" ? "farmerConfirmed" : "supplierConfirmed";
    if (verification[flag] === true) return response(verification);
    if (action === "verify") {
      if (!["AWAITING_PICKUP", "IN_TRANSIT", "DELIVERED"].includes(order.status)) {
        throw new Error("Order is not available for handover verification");
      }
      if (verification.expiresAt <= now || typeof verification.code !== "string") {
        throw new Error("Pickup code expired. Ask the Supplier to refresh it before confirming pickup.");
      }
      const windowStarted = verification.attemptWindowStarted ?? now;
      const attempts = now - windowStarted >= ATTEMPT_WINDOW_MS ? 0 : verification.attempts ?? 0;
      if (attempts >= MAX_ATTEMPTS) return { ...response(verification), error: "Too many attempts. Try again after 30 minutes." };
      const valid = typeof submittedCode === "string" && /^\d{6}$/.test(submittedCode) &&
        timingSafeEqual(Buffer.from(submittedCode), Buffer.from(verification.code));
      if (!valid) {
        tx.update(verificationRef, { attempts: attempts + 1,
          attemptWindowStarted: attempts === 0 ? now : windowStarted });
        // Return an error so the failed-attempt counter commits instead of rolling back.
        return { ...response(verification), error: "Incorrect pickup code. Check the code with the Supplier." };
      }
    } else {
      if (order.status !== "AWAITING_PICKUP") throw new Error("Order must be awaiting pickup");
      if (!verification.farmerConfirmed && verification.expiresAt <= now) {
        throw new Error("Pickup code expired. Refresh pickup verification before confirming collection.");
      }
    }
    const points = user.points ?? 0;
    if (!Number.isSafeInteger(points) || points < 0 || points >= Number.MAX_SAFE_INTEGER) {
      throw new Error("Invalid participant points");
    }
    tx.update(userRef, { points: points + ORDER_PICKUP_POINTS });
    const changes = { [flag]: true, [`${flag}At`]: now,
      ...(action === "verify" ? { code: null } : {}) };
    tx.update(verificationRef, changes);
    if (action === "collect") tx.update(orderRef, { status: "IN_TRANSIT", updatedAt: now });
    return response({ ...verification, ...changes });
  });
}

/** All lifecycle scoring uses the existing users.points field and role-specific ranking. */
export const ORDER_SCORE = Object.freeze({ farmerAccept: 1, farmerHandover: 3, supplierPickup: 4, complete: 6, cancel: -2 });
export function scoreAfter(points: unknown, delta: number): number {
  const current = points ?? 0;
  if (typeof current !== 'number' || !Number.isSafeInteger(current) || current < 0 || current > 2147483647) {
    throw new Error('Invalid participant points');
  }
  return Math.min(2147483647, Math.max(0, current + delta));
}

/** Invoked ONLY by the order onCreate trigger, never by screen opens or client claims. */
export async function awardOrderAcceptance(db: FirebaseFirestore.Firestore, orderId: string) {
  return db.runTransaction(async tx => {
    const order = (await tx.get(db.doc(`orders/${orderId}`))).data();
    if (!order || order.orderId !== orderId) throw new Error('Order not found');
    const receiptRef = db.doc(`order_pickup_verifications/${orderId}`);
    const farmerRef = db.doc(`users/${order.farmerId}`);
    const sourceRef = order.bidId ? db.doc(`bids/${order.bidId}`) : db.doc(`order_requests/${orderId}`);
    const [receipt, farmerSnapshot, sourceSnapshot] = await Promise.all([tx.get(receiptRef), tx.get(farmerRef), tx.get(sourceRef)]);
    if (receipt.get('farmerAcceptRewarded') === true) return { alreadyAwarded: true };
    const farmer = farmerSnapshot.data(), source = sourceSnapshot.data();
    // Match the committed proposal; acceptance is not awarded for rejected/expired requests.
    const sourceQuantity = order.bidId ? order.quantity : source?.requestedQuantity;
    const sourcePrice = order.bidId ? source?.offerPrice : source?.listedPrice;
    if (farmer?.userId !== order.farmerId || farmer?.role !== 'FARMER' || source?.status !== 'ACCEPTED' ||
        source?.listingId !== order.listingId || source?.supplierId !== order.supplierId ||
        (!order.bidId && source?.farmerId !== order.farmerId) ||
        sourceQuantity !== order.quantity || sourcePrice !== order.agreedPrice) {
      throw new Error('Accepted proposal does not match the order');
    }
    // A cancellation can beat this asynchronous trigger. Offset only this late acceptance
    // reward against the unconsumed penalty, preserving accept-then-cancel floor semantics.
    const cancellationOffset = receipt.get('cancelledBy') === order.farmerId
      ? Math.min(ORDER_SCORE.farmerAccept, Math.max(0, -ORDER_SCORE.cancel - (receipt.get('cancellationPenaltyApplied') ?? 0))) : 0;
    tx.update(farmerRef, { points: scoreAfter(farmer.points, ORDER_SCORE.farmerAccept - cancellationOffset) });
    tx.set(receiptRef, { farmerAcceptRewarded: true, farmerAcceptRewardedAt: Date.now(),
      ...(cancellationOffset ? { cancellationPenaltyApplied: (receipt.get('cancellationPenaltyApplied') ?? 0) + cancellationOffset } : {})
    }, { merge: true });
    return { alreadyAwarded: false };
  });
}

/** User-initiated cancellation, stock release, penalty and receipt commit together. */
export async function cancelOrderTransaction(db: FirebaseFirestore.Firestore, orderId: string, userId: string) {
  return db.runTransaction(async tx => {
    const orderRef = db.doc(`orders/${orderId}`);
    const order = (await tx.get(orderRef)).data();
    if (!order || order.orderId !== orderId) throw new Error('Order not found');
    const role = userId === order.farmerId ? 'FARMER' : userId === order.supplierId ? 'SUPPLIER' : null;
    if (!role) throw new Error('Only an order participant can cancel');
    const userRef = db.doc(`users/${userId}`), receiptRef = db.doc(`order_pickup_verifications/${orderId}`);
    const [userSnapshot, receipt] = await Promise.all([tx.get(userRef), tx.get(receiptRef)]);
    const user = userSnapshot.data();
    if (user?.userId !== userId || user.role !== role) throw new Error('Order participant profile does not match');
    if (order.status === 'CANCELLED') {
      if (receipt.get('cancelledBy') === userId) return { alreadyCancelled: true };
      throw new Error('This order has already been cancelled');
    }
    if (order.status !== 'AWAITING_PICKUP') throw new Error('Orders cannot be cancelled after pickup');
    const listingRef = db.doc(`listings/${order.listingId}`);
    const listing = (await tx.get(listingRef)).data();
    if (listing) {
      if (listing.farmerId !== order.farmerId || !(order.quantity > 0)) throw new Error('Invalid reserved stock');
      const sold = Math.max(0, (listing.soldQuantity ?? 0) - order.quantity);
      const available = listing.availableQuantity ?? Math.max(0, listing.quantity - (listing.soldQuantity ?? 0));
      const released = Math.min(available + order.quantity, Math.max(0, listing.quantity - sold));
      const reopen = listing.status === 'FULFILLED' && released > 0;
      tx.update(listingRef, { soldQuantity: sold, availableQuantity: released,
        ...(reopen ? { status: 'OPEN', biddingEnabled: true } : {}) });
    }
    const points = scoreAfter(user.points, ORDER_SCORE.cancel);
    tx.update(userRef, { points });
    tx.update(orderRef, { status: 'CANCELLED', updatedAt: Date.now() });
    tx.set(receiptRef, { cancelledBy: userId, cancellationPenaltyAt: Date.now(),
      cancellationPenaltyApplied: (user.points ?? 0) - points }, { merge: true });
    return { alreadyCancelled: false };
  });
}

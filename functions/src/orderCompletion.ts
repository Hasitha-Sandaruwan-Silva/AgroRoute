import { ORDER_SCORE, scoreAfter } from "./orderScoring";
/** Legitimate delivery earns its separate +6; pickup rewards are never repeated. */
export async function completeOrderTransaction(db: FirebaseFirestore.Firestore, orderId: string, userId: string) {
  const orderRef = db.collection("orders").doc(orderId);
  const receiptRef = db.collection("order_completion_records").doc(orderId);
  return db.runTransaction(async (transaction) => {
    const orderSnapshot = await transaction.get(orderRef);
    const order = orderSnapshot.data();
    if (!order || order.orderId !== orderId) throw new Error("Order not found");
    if (order.supplierId !== userId) throw new Error("Only the order's supplier can complete it");
    const farmerRef = db.collection("users").doc(order.farmerId);
    const supplierRef = db.collection("users").doc(order.supplierId);
    const [farmerSnapshot, supplierSnapshot, receiptSnapshot] = await Promise.all([
      transaction.get(farmerRef), transaction.get(supplierRef), transaction.get(receiptRef),
    ]);
    const farmer = farmerSnapshot.data();
    const supplier = supplierSnapshot.data();
    if (farmer?.userId !== order.farmerId || farmer?.role !== "FARMER" ||
        supplier?.userId !== userId || supplier?.role !== "SUPPLIER") {
      throw new Error("Order participant profiles do not match");
    }
    if (receiptSnapshot.exists) {
      const receipt = receiptSnapshot.data();
      if (order.status !== "DELIVERED" || receipt?.farmerId !== order.farmerId ||
          receipt?.supplierId !== order.supplierId || receipt?.orderId !== orderId) {
        throw new Error("Completion receipt does not match the order");
      }
      return { orderId, alreadyCompleted: true };
    }
    if (order.status !== "IN_TRANSIT") throw new Error("Order must be in transit before completion");
    const now = Date.now();
    const farmerPoints = scoreAfter(farmer.points, ORDER_SCORE.complete);
    const supplierPoints = scoreAfter(supplier.points, ORDER_SCORE.complete);
    transaction.update(farmerRef, { points: farmerPoints });
    transaction.update(supplierRef, { points: supplierPoints });
    transaction.update(orderRef, { status: "DELIVERED", updatedAt: now });
    transaction.create(receiptRef, {
      orderId, farmerId: order.farmerId, supplierId: order.supplierId, completedAt: now,
      farmerCompletionPoints: ORDER_SCORE.complete, supplierCompletionPoints: ORDER_SCORE.complete,
    });
    return { orderId, alreadyCompleted: false };
  });
}

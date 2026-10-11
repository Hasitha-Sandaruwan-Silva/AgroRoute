const test = require("node:test");
const assert = require("node:assert/strict");
const {
  resolveNotificationRecipient,
  routeForNotification,
} = require("../lib/notificationPolicy.js");

test("request events resolve the counterparty and role", () => {
  const request = {
    requestId: "request-7",
    farmerId: "farmer-1",
    supplierId: "supplier-2",
    status: "PENDING",
    updatedAt: 12,
  };
  const created = resolveNotificationRecipient("ORDER_REQUEST_CREATED", "supplier-2", { request });
  assert.deepEqual(created, {
    userId: "farmer-1",
    role: "FARMER",
    entityId: "request-7",
    version: "12",
  });
  assert.equal(resolveNotificationRecipient("ORDER_REQUEST_CREATED", "farmer-1", { request }), null);
  assert.equal(routeForNotification("ORDER_REQUEST_CREATED", "FARMER", {}), "farmer_order_requests");
});

test("bid and order events never resolve back to their actor", () => {
  const bid = { bidId: "bid-3", supplierId: "supplier-2", status: "PENDING", updatedAt: 5 };
  const listing = { listingId: "listing-4", farmerId: "farmer-1", harvestStatus: "READY" };
  const recipient = resolveNotificationRecipient("BID_CREATED", "supplier-2", { bid, listing });
  assert.equal(recipient.userId, "farmer-1");
  assert.equal(resolveNotificationRecipient("BID_CREATED", "farmer-1", { bid, listing }), null);

  const order = {
    orderId: "order-5",
    farmerId: "farmer-1",
    supplierId: "supplier-2",
    status: "IN_TRANSIT",
    updatedAt: 10,
  };
  assert.equal(resolveNotificationRecipient("ORDER_IN_TRANSIT", "supplier-2", { order }).userId, "farmer-1");
  assert.equal(resolveNotificationRecipient("ORDER_IN_TRANSIT", "farmer-1", { order }), null);
  assert.equal(routeForNotification("ORDER_IN_TRANSIT", "SUPPLIER", { orderId: "order-5" }), "supplier_order_detail/order-5");
});

test("persisted state gates request changes and harvest notices", () => {
  const rejectedRequest = {
    requestId: "request-7",
    farmerId: "farmer-1",
    supplierId: "supplier-2",
    status: "REJECTED",
    updatedAt: 20,
  };
  const rejected = resolveNotificationRecipient("ORDER_REQUEST_REJECTED", "farmer-1", { request: rejectedRequest });
  assert.equal(rejected.userId, "supplier-2");
  assert.equal(routeForNotification("ORDER_REQUEST_REJECTED", "SUPPLIER", {}), "supplier_orders");
  assert.equal(resolveNotificationRecipient("ORDER_REQUEST_REJECTED", "supplier-2", { request: rejectedRequest }), null);

  const harvest = {
    listingId: "listing-4",
    farmerId: "farmer-1",
    harvestStatus: "READY",
    harvestUpdatedAt: 30,
  };
  const relatedRequest = {
    requestId: "request-8",
    supplierId: "supplier-2",
    status: "PENDING",
    isPreHarvest: true,
  };
  assert.equal(resolveNotificationRecipient("HARVEST_READY", "farmer-1", { listing: harvest, relatedRequest }).userId, "supplier-2");
  assert.equal(resolveNotificationRecipient("HARVEST_READY", "supplier-2", { listing: harvest, relatedRequest }), null);
});

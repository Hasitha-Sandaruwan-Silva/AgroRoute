const { test, after } = require('node:test');
const assert = require('node:assert/strict');
const admin = require('firebase-admin');
const { ORDER_SCORE, awardOrderAcceptance, cancelOrderTransaction } = require('../lib/orderScoring');
const { orderPickupTransaction } = require('../lib/orderPickup');
const { completeOrderTransaction } = require('../lib/orderCompletion');
if (!/^127\.0\.0\.1:\d+$/.test(process.env.FIRESTORE_EMULATOR_HOST || '')) throw new Error('Local emulator required');
const app = admin.initializeApp({ projectId: 'demo-agroroute-rules' }, 'scoring-test');
const db = app.firestore();
let sequence = 0;
async function seed({ status = 'AWAITING_PICKUP', farmerPoints = 12, supplierPoints = 7, requestStatus = 'ACCEPTED' } = {}) {
  const id = `score-${++sequence}`, farmer = `${id}-farmer`, supplier = `${id}-supplier`, listing = `${id}-listing`;
  const order = { orderId: id, listingId: listing, bidId: '', farmerId: farmer, supplierId: supplier,
    status, quantity: 5, agreedPrice: 25, pickupLocation: 'original pickup', deliveryLocation: 'original delivery' };
  await db.doc(`users/${farmer}`).set({ userId: farmer, role: 'FARMER', points: farmerPoints });
  await db.doc(`users/${supplier}`).set({ userId: supplier, role: 'SUPPLIER', points: supplierPoints });
  await db.doc(`listings/${listing}`).set({ farmerId: farmer, quantity: 10, soldQuantity: 5, availableQuantity: 5, status: 'OPEN', biddingEnabled: false });
  await db.doc(`order_requests/${id}`).set({ requestId: id, listingId: listing, farmerId: farmer,
    supplierId: supplier, status: requestStatus, requestedQuantity: 5, listedPrice: 25 });
  await db.doc(`orders/${id}`).set(order);
  return { id, farmer, supplier, listing, order };
}
const points = async uid => (await db.doc(`users/${uid}`).get()).get('points');
const pickup = (f, uid, action, code) => orderPickupTransaction(db, f.id, uid, action, code);
after(() => app.delete());

test('committed acceptance grants farmer one exactly once under concurrent trigger retries; existing scores preserved', async () => {
  const f = await seed();
  const results = await Promise.all(Array.from({ length: 5 }, () => awardOrderAcceptance(db, f.id)));
  assert.equal(results.filter(r => !r.alreadyAwarded).length, 1);
  assert.equal(await points(f.farmer), 13);
  assert.equal(await points(f.supplier), 7);
  assert.equal((await db.doc(`orders/${f.id}`).get()).get('status'), 'AWAITING_PICKUP');
});
test('full lifecycle totals ten per role including delivery retries and independent actions', async () => {
  const f = await seed();
  await awardOrderAcceptance(db, f.id);
  const { code } = await pickup(f, f.supplier, 'view');
  assert.equal(await points(f.farmer), 13);
  assert.equal(await points(f.supplier), 7);
  const wrong = code === '000000' ? '111111' : '000000';
  assert.ok((await pickup(f, f.farmer, 'verify', wrong)).error);
  assert.equal(await points(f.farmer), 13);
  await Promise.all(Array.from({ length: 5 }, () => pickup(f, f.farmer, 'verify', code)));
  assert.equal(await points(f.farmer), 16);
  assert.equal(await points(f.supplier), 7);
  await Promise.all(Array.from({ length: 5 }, () => pickup(f, f.supplier, 'collect')));
  assert.equal(await points(f.supplier), 11);
  assert.equal(await points(f.farmer), 16);
  await Promise.all(Array.from({ length: 5 }, () => completeOrderTransaction(db, f.id, f.supplier)));
  assert.equal(await points(f.farmer), 22);
  assert.equal(await points(f.supplier), 17);
  await awardOrderAcceptance(db, f.id);
  await pickup(f, f.farmer, 'verify', code);
  await pickup(f, f.supplier, 'collect');
  assert.equal(await points(f.farmer), 22);
  assert.equal(await points(f.supplier), 17);
  const order = (await db.doc(`orders/${f.id}`).get()).data();
  assert.equal(order.pickupLocation, f.order.pickupLocation);
  assert.equal(order.deliveryLocation, f.order.deliveryLocation);
});
test('farmer and supplier cancellation each penalize only cancelling user and restore stock once', async () => {
  for (const role of ['farmer', 'supplier']) {
    const f = await seed();
    const results = await Promise.all(Array.from({ length: 5 }, () => cancelOrderTransaction(db, f.id, f[role])));
    assert.equal(results.filter(r => !r.alreadyCancelled).length, 1);
    assert.equal(await points(f.farmer), role === 'farmer' ? 10 : 12);
    assert.equal(await points(f.supplier), role === 'supplier' ? 5 : 7);
    const listing = (await db.doc(`listings/${f.listing}`).get()).data();
    assert.equal(listing.availableQuantity, 10);
    assert.equal(listing.soldQuantity, 0);
    await assert.rejects(cancelOrderTransaction(db, f.id, role === 'farmer' ? f.supplier : f.farmer));
    await assert.rejects(completeOrderTransaction(db, f.id, f.supplier));
    await assert.rejects(pickup(f, f.supplier, 'collect'));
  }
});
test('score floors at zero and trigger/cancellation ordering does not change the final score', async () => {
  for (const initial of [0, 1, 5]) {
    for (const order of ['accept-first', 'cancel-first', 'concurrent']) {
      const f = await seed({ farmerPoints: initial, supplierPoints: 1 });
      const accept = () => awardOrderAcceptance(db, f.id);
      const cancel = () => cancelOrderTransaction(db, f.id, f.farmer);
      if (order === 'accept-first') { await accept(); await cancel(); }
      else if (order === 'cancel-first') { await cancel(); await accept(); }
      else await Promise.all([accept(), cancel()]);
      assert.equal(await points(f.farmer), Math.max(0, initial + 1 - 2));
      assert.equal(await points(f.supplier), 1);
    }
  }
  const f = await seed({ supplierPoints: 1 });
  await cancelOrderTransaction(db, f.id, f.supplier);
  assert.equal(await points(f.supplier), 0);
});
test('transit/delivered cancellation blocked for both roles; unauthorized caller cannot trigger penalty', async () => {
  for (const status of ['IN_TRANSIT', 'DELIVERED']) {
    const f = await seed({ status });
    await assert.rejects(cancelOrderTransaction(db, f.id, f.farmer));
    await assert.rejects(cancelOrderTransaction(db, f.id, f.supplier));
    assert.equal(await points(f.farmer), 12);
    assert.equal(await points(f.supplier), 7);
  }
  const f = await seed();
  await assert.rejects(cancelOrderTransaction(db, f.id, 'other-user'));
  assert.equal((await db.doc(`orders/${f.id}`).get()).get('status'), 'AWAITING_PICKUP');
});
test('rejected, expired, and pending requests never earn acceptance or cancellation penalties', async () => {
  for (const status of ['REJECTED', 'EXPIRED', 'PENDING', 'CANCELLED']) {
    const f = await seed({ requestStatus: status });
    await assert.rejects(awardOrderAcceptance(db, f.id));
    assert.equal(await points(f.farmer), 12);
    assert.equal(await points(f.supplier), 7);
    await db.doc(`orders/${f.id}`).delete(); // Requests alone are not cancellable orders.
    await assert.rejects(cancelOrderTransaction(db, f.id, f.supplier));
    assert.equal(await points(f.supplier), 7);
  }
});
test('opening historical completed orders does not award points; historical receipts suppress new completion rewards', async () => {
  const f = await seed({ status: 'DELIVERED' });
  await db.doc(`order_completion_records/${f.id}`).set({ orderId: f.id, farmerId: f.farmer, supplierId: f.supplier, pointsAwardedAt: 1 });
  await pickup(f, f.supplier, 'view');
  await pickup(f, f.farmer, 'view');
  await completeOrderTransaction(db, f.id, f.supplier);
  assert.equal(await points(f.farmer), 12);
  assert.equal(await points(f.supplier), 7);
});
test('pickup versus cancellation race commits exactly one eligible path', async () => {
  const f = await seed();
  await pickup(f, f.supplier, 'view');
  const results = await Promise.allSettled([pickup(f, f.supplier, 'collect'), cancelOrderTransaction(db, f.id, f.supplier)]);
  assert.equal(results.filter(r => r.status === 'fulfilled').length, 1);
  const order = (await db.doc(`orders/${f.id}`).get()).data();
  assert.equal(await points(f.supplier), order.status === 'IN_TRANSIT' ? 11 : 5);
});

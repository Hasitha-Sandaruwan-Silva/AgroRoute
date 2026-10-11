const { test, after } = require('node:test');
const assert = require('node:assert/strict');
const admin = require('firebase-admin');
const { orderPickupTransaction, ORDER_PICKUP_POINTS } = require('../lib/orderPickup');
const { completeOrderTransaction } = require('../lib/orderCompletion');
if (!/^127\.0\.0\.1:\d+$/.test(process.env.FIRESTORE_EMULATOR_HOST || '')) {
  throw new Error('Pickup tests require a local emulator. Never run against production.');
}
const app = admin.initializeApp({ projectId: 'demo-agroroute-rules' }, 'pickup-test');
const db = app.firestore();
let counter = 0;
async function seed(status = 'AWAITING_PICKUP') {
  const id = `pickup-${++counter}`, farmer = `${id}-farmer`, supplier = `${id}-supplier`;
  await db.doc(`users/${farmer}`).set({ userId: farmer, role: 'FARMER', points: 5, phone: 'unchanged' });
  await db.doc(`users/${supplier}`).set({ userId: supplier, role: 'SUPPLIER', points: 2 });
  const order = { orderId: id, farmerId: farmer, supplierId: supplier, status,
    pickupLocation: 'Original farm\n[7.000000, 80.000000]',
    deliveryLocation: 'Original destination\n[6.000000, 79.000000]' };
  await db.doc(`orders/${id}`).set(order);
  return { id, farmer, supplier, order };
}
const call = (f, user, action, code) => orderPickupTransaction(db, f.id, user, action, code);
const points = async uid => (await db.doc(`users/${uid}`).get()).get('points');
after(() => app.delete());

test('supplier-only random six-digit code viewing never awards points; farmer cannot read code', async () => {
  const f = await seed();
  assert.equal((await call(f, f.farmer, 'view')).code, null);
  const supplierView = await call(f, f.supplier, 'view');
  assert.match(supplierView.code, /^\d{6}$/);
  assert.equal((await call(f, f.supplier, 'view')).code, supplierView.code);
  assert.equal((await call(f, f.farmer, 'view')).code, null);
  assert.equal(await points(f.farmer), 5);
  assert.equal(await points(f.supplier), 2);
});
test('correct farmer verification awards only farmer once under concurrent retries and leaves status unchanged', async () => {
  const f = await seed(), { code } = await call(f, f.supplier, 'view');
  const result = await Promise.all(Array.from({ length: 6 }, () => call(f, f.farmer, 'verify', code)));
  assert.ok(result.every(r => r.farmerConfirmed && !r.supplierConfirmed));
  assert.equal(await points(f.farmer), 5 + ORDER_PICKUP_POINTS);
  assert.equal(await points(f.supplier), 2);
  assert.equal((await db.doc(`orders/${f.id}`).get()).get('status'), 'AWAITING_PICKUP');
  assert.equal((await db.doc(`order_pickup_verifications/${f.id}`).get()).get('code'), null);
  assert.equal((await call(f, f.farmer, 'view')).farmerConfirmed, true);
});
test('wrong codes commit rate limits and cannot award points or confirm handover', async () => {
  const f = await seed(), { code } = await call(f, f.supplier, 'view');
  const wrong = code === '000000' ? '111111' : '000000';
  for (let i = 0; i < 5; i++) assert.match((await call(f, f.farmer, 'verify', wrong)).error, /Incorrect/);
  assert.match((await call(f, f.farmer, 'verify', code)).error, /Too many/);
  assert.equal(await points(f.farmer), 5);
  assert.equal((await call(f, f.farmer, 'view')).farmerConfirmed, false);
  await db.doc(`order_pickup_verifications/${f.id}`).update({ attemptWindowStarted: Date.now() - 31 * 60 * 1000 });
  assert.equal((await call(f, f.farmer, 'verify', code)).farmerConfirmed, true);
});
test('supplier collection awards only supplier once, moves to IN_TRANSIT, and permits later farmer verification', async () => {
  const f = await seed(), { code } = await call(f, f.supplier, 'view');
  const results = await Promise.all(Array.from({ length: 6 }, () => call(f, f.supplier, 'collect')));
  assert.ok(results.every(r => r.supplierConfirmed && !r.farmerConfirmed));
  assert.equal(await points(f.supplier), 2 + ORDER_PICKUP_POINTS);
  assert.equal(await points(f.farmer), 5);
  assert.equal((await db.doc(`orders/${f.id}`).get()).get('status'), 'IN_TRANSIT');
  assert.equal((await call(f, f.supplier, 'view')).code, code);
  assert.equal((await call(f, f.farmer, 'verify', code)).farmerConfirmed, true);
  assert.equal(await points(f.farmer), 5 + ORDER_PICKUP_POINTS);
});
test('independent simultaneous confirmations and delivery never duplicate pickup rewards or mutate locations', async () => {
  const f = await seed(), { code } = await call(f, f.supplier, 'view');
  await Promise.all([call(f, f.farmer, 'verify', code), call(f, f.supplier, 'collect')]);
  await Promise.all(Array.from({ length: 4 }, () => completeOrderTransaction(db, f.id, f.supplier)));
  await call(f, f.farmer, 'verify', code);
  await call(f, f.supplier, 'collect');
  const order = (await db.doc(`orders/${f.id}`).get()).data();
  assert.equal(order.status, 'DELIVERED');
  assert.equal(order.pickupLocation, f.order.pickupLocation);
  assert.equal(order.deliveryLocation, f.order.deliveryLocation);
  assert.equal(await points(f.farmer), 6);
  assert.equal(await points(f.supplier), 3);
});
test('supplier delivery does not prevent still-valid independent farmer verification', async () => {
  const f = await seed(), { code } = await call(f, f.supplier, 'view');
  await call(f, f.supplier, 'collect');
  await completeOrderTransaction(db, f.id, f.supplier);
  assert.equal(await points(f.farmer), 5);
  await call(f, f.farmer, 'verify', code);
  assert.equal(await points(f.farmer), 6);
});
test('unauthorized participants, incorrect roles, and cross-order code are rejected without rewards', async () => {
  const f = await seed(), other = await seed();
  const { code } = await call(f, f.supplier, 'view');
  await call(other, other.supplier, 'view');
  await db.doc(`order_pickup_verifications/${other.id}`).update({ code: code === '000000' ? '111111' : '000000' });
  for (const action of ['view', 'verify', 'collect']) await assert.rejects(call(f, other.supplier, action, code));
  await assert.rejects(call(f, f.supplier, 'verify', code));
  await assert.rejects(call(f, f.farmer, 'collect'));
  assert.match((await call(other, other.farmer, 'verify', code)).error, /Incorrect/);
  await db.doc(`users/${f.farmer}`).update({ role: 'SUPPLIER' });
  await assert.rejects(call(f, f.farmer, 'verify', code));
  assert.equal(await points(f.farmer), 5);
  assert.equal(await points(f.supplier), 2);
});
test('expired codes rotate only before pickup, remain temporary, and invalid point balances do not partially confirm', async () => {
  const f = await seed();
  await call(f, f.supplier, 'view');
  await db.doc(`order_pickup_verifications/${f.id}`).update({ expiresAt: Date.now() - 1 });
  await assert.rejects(call(f, f.farmer, 'verify', '000000'), /expired/);
  const { code } = await call(f, f.supplier, 'view');
  assert.match(code, /^\d{6}$/);
  await db.doc(`users/${f.farmer}`).update({ points: -1 });
  await assert.rejects(call(f, f.farmer, 'verify', code), /points/);
  assert.equal((await call(f, f.supplier, 'view')).farmerConfirmed, false);
});
test('legacy transit/delivered/cancelled orders cannot earn new pickup rewards and legacy transit can finish', async () => {
  for (const status of ['IN_TRANSIT', 'DELIVERED', 'CANCELLED']) {
    const f = await seed(status);
    assert.equal((await call(f, f.supplier, 'view')).legacy, true);
    await assert.rejects(call(f, f.supplier, 'collect'));
    await assert.rejects(call(f, f.farmer, 'verify', '000000'));
    if (status === 'IN_TRANSIT') await completeOrderTransaction(db, f.id, f.supplier);
    assert.equal(await points(f.farmer), 5);
    assert.equal(await points(f.supplier), 2);
  }
});
test('cancelled enrolled orders cannot verify or collect, and legacy completion receipt cannot re-enroll', async () => {
  const f = await seed(), { code } = await call(f, f.supplier, 'view');
  await db.doc(`orders/${f.id}`).update({ status: 'CANCELLED' });
  await assert.rejects(call(f, f.farmer, 'verify', code));
  await assert.rejects(call(f, f.supplier, 'collect'));
  const old = await seed();
  await db.doc(`order_completion_records/${old.id}`).set({ orderId: old.id });
  assert.equal((await call(old, old.supplier, 'view')).code, null);
  await assert.rejects(call(old, old.supplier, 'collect'));
});
test('existing role-filtered points ordering reflects independent pickup reward', async () => {
  const f = await seed(), { code } = await call(f, f.supplier, 'view');
  await call(f, f.farmer, 'verify', code);
  const ranked = await db.collection('users').where('role', '==', 'FARMER').orderBy('points', 'desc').get();
  const values = ranked.docs.map(doc => doc.get('points'));
  assert.ok(values.includes(6));
  assert.deepEqual(values, [...values].sort((a, b) => b - a));
});

test('expired post-pickup code can be renewed by owning supplier without rewards or legacy enrollment', async () => {
  const f = await seed();
  await call(f, f.supplier, 'view');
  await call(f, f.supplier, 'collect');
  await completeOrderTransaction(db, f.id, f.supplier);
  await db.doc(`order_pickup_verifications/${f.id}`).update({ expiresAt: Date.now() - 1 });
  const renewed = await call(f, f.supplier, 'view');
  assert.match(renewed.code, /^\d{6}$/);
  assert.equal(await points(f.supplier), 3);
  assert.equal(await points(f.farmer), 5);
  await call(f, f.farmer, 'verify', renewed.code);
  assert.equal(await points(f.farmer), 6);
});
test('concurrent incorrect attempts cannot bypass five-attempt limit', async () => {
  const f = await seed(), { code } = await call(f, f.supplier, 'view');
  const wrong = code === '000000' ? '111111' : '000000';
  await Promise.all(Array.from({ length: 5 }, () => call(f, f.farmer, 'verify', wrong)));
  assert.equal((await db.doc(`order_pickup_verifications/${f.id}`).get()).get('attempts'), 5);
  assert.match((await call(f, f.farmer, 'verify', code)).error, /Too many/);
  assert.equal(await points(f.farmer), 5);
});

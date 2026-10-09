const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const admin = require('firebase-admin');
const { completeOrderTransaction } = require('../lib/orderCompletion');

if (!/^127\.0\.0\.1:\d+$/.test(process.env.FIRESTORE_EMULATOR_HOST || '')) {
  throw new Error('These tests require a local Firestore emulator. Never run against production.');
}
const app = admin.initializeApp({ projectId: 'demo-agroroute-rules' }, 'completion-test');
const db = app.firestore();
let counter = 0;
async function seed(status = 'IN_TRANSIT') {
  const id = `completion-${++counter}`;
  const farmerId = `${id}-farmer`, supplierId = `${id}-supplier`;
  await db.doc(`users/${farmerId}`).set({ userId: farmerId, role: 'FARMER', points: 5, phone: '+94000000001' });
  await db.doc(`users/${supplierId}`).set({ userId: supplierId, role: 'SUPPLIER', points: 2 });
  await db.doc(`orders/${id}`).set({ orderId: id, farmerId, supplierId, status });
  return { id, farmerId, supplierId };
}
after(() => app.delete());
test('completion awards one point per participant and preserves profile fields', async () => {
  const { id, farmerId, supplierId } = await seed();
  assert.equal((await completeOrderTransaction(db, id, supplierId)).alreadyCompleted, false);
  assert.equal((await db.doc(`orders/${id}`).get()).get('status'), 'DELIVERED');
  assert.equal((await db.doc(`users/${farmerId}`).get()).get('points'), 6);
  assert.equal((await db.doc(`users/${farmerId}`).get()).get('phone'), '+94000000001');
  assert.equal((await db.doc(`users/${supplierId}`).get()).get('points'), 3);
  assert.equal((await db.doc(`order_completion_records/${id}`).get()).get('orderId'), id);
});
test('concurrent retries award once', async () => {
  const { id, farmerId, supplierId } = await seed();
  const results = await Promise.all(Array.from({ length: 4 }, () => completeOrderTransaction(db, id, supplierId)));
  assert.equal(results.filter(r => !r.alreadyCompleted).length, 1);
  assert.equal((await db.doc(`users/${farmerId}`).get()).get('points'), 6);
  assert.equal((await db.doc(`users/${supplierId}`).get()).get('points'), 3);
});
test('farmer and unrelated supplier cannot complete order', async () => {
  const { id, farmerId } = await seed();
  await assert.rejects(completeOrderTransaction(db, id, farmerId));
  await assert.rejects(completeOrderTransaction(db, id, 'other-supplier'));
  assert.equal((await db.doc(`orders/${id}`).get()).get('status'), 'IN_TRANSIT');
});
test('awaiting pickup, cancelled and delivered without receipt cannot award points', async () => {
  for (const status of ['AWAITING_PICKUP', 'CANCELLED', 'DELIVERED']) {
    const { id, supplierId, farmerId } = await seed(status);
    await assert.rejects(completeOrderTransaction(db, id, supplierId));
    assert.equal((await db.doc(`users/${farmerId}`).get()).get('points'), 5);
  }
});
test('invalid role and negative points reject without partial writes', async () => {
  const { id, farmerId, supplierId } = await seed();
  await db.doc(`users/${supplierId}`).update({ role: 'FARMER' });
  await assert.rejects(completeOrderTransaction(db, id, supplierId));
  await db.doc(`users/${supplierId}`).update({ role: 'SUPPLIER' });
  await db.doc(`users/${farmerId}`).update({ points: -1 });
  await assert.rejects(completeOrderTransaction(db, id, supplierId));
  assert.equal((await db.doc(`orders/${id}`).get()).get('status'), 'IN_TRANSIT');
  assert.equal((await db.doc(`order_completion_records/${id}`).get()).exists, false);
});

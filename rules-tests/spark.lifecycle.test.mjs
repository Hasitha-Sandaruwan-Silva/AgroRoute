import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { randomInt, randomBytes, pbkdf2Sync, timingSafeEqual } from 'node:crypto';
import { before, beforeEach, after, test } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc, deleteDoc, writeBatch, runTransaction, serverTimestamp, Timestamp } from 'firebase/firestore';

if (!/^127\.0\.0\.1:\d+$/.test(process.env.FIRESTORE_EMULATOR_HOST || '')) throw Error('Local demo emulator required');
let env;
const db = uid => env.authenticatedContext(uid).firestore();
const admin = async fn => env.withSecurityRulesDisabled(c => fn(c.firestore()));
const order = { orderId: 'o', listingId: 'l', bidId: '', farmerId: 'f', supplierId: 's', cropName: 'Rice',
  quantity: 5, quantityUnit: 'KG', agreedPrice: 8, status: 'AWAITING_PICKUP',
  pickupLocation: 'Farm\n[7, 80]', deliveryLocation: 'Depot\n[6, 79]', createdAt: 1, updatedAt: 1, sparkVersion: 1 };
before(async () => {
  const [host, port] = process.env.FIRESTORE_EMULATOR_HOST.split(':');
  env = await initializeTestEnvironment({ projectId: 'demo-agroroute-rules', firestore: {
    host, port: +port, rules: await readFile(new URL('../firestore.rules', import.meta.url), 'utf8') } });
});
after(async () => env?.cleanup());
beforeEach(async () => {
  await env.clearFirestore();
  await admin(async d => {
    for (const [uid, role] of [['f','FARMER'],['f2','FARMER'],['s','SUPPLIER'],['s2','SUPPLIER']])
      await setDoc(doc(d, 'users', uid), { userId: uid, role, points: 0 });
    await setDoc(doc(d, 'listings/l'), { listingId: 'l', farmerId: 'f', cropName: 'Rice', quantityUnit: 'KG',
      quantity: 10, availableQuantity: 10, soldQuantity: 0, status: 'OPEN', harvestStatus: 'READY',
      biddingEnabled: false, reservePrice: 8, location: order.pickupLocation, isDraft: false });
    await setDoc(doc(d, 'order_requests/o'), { requestId: 'o', listingId: 'l', farmerId: 'f', supplierId: 's',
      cropName: 'Rice', quantityUnit: 'KG', requestedQuantity: 5, listedPrice: 8, status: 'PENDING', deliveryLocation: order.deliveryLocation });
  });
});
function score(batch, d, uid, type, delta, old, overrides = {}) {
  const id = type === 'cancellation' ? `o_${uid}_cancellation` : `o_${type}`;
  batch.set(doc(d, 'scoreEvents', id), { orderId: 'o', userId: uid, role: uid.startsWith('f') ? 'FARMER' : 'SUPPLIER',
    eventType: type, pointsDelta: delta, createdAt: serverTimestamp(), ...overrides });
  batch.update(doc(d, 'users', uid), { points: Math.max(0, old + delta), lastScoreEventId: id });
}
function acceptBatch(uid = 'f', overrides = {}) {
  const d = db(uid), b = writeBatch(d);
  b.set(doc(d, 'orders/o'), { ...order, ...overrides });
  b.update(doc(d, 'order_requests/o'), { status: 'ACCEPTED', updatedAt: 2 });
  b.update(doc(d, 'listings/l'), { availableQuantity: 5, soldQuantity: 5, stockOrderId: 'o' });
  score(b, d, 'f', 'farmer_acceptance', 1, 0);
  return b;
}
async function accept() { await assertSucceeds(acceptBatch().commit()); }
function pin() {
  const code = randomInt(1000000).toString().padStart(6,'0'), salt = randomBytes(16).toString('hex');
  return { code, salt, verifier: pbkdf2Sync(code, Buffer.from(salt,'hex'), 120000,32,'sha256').toString('hex') };
}
function enrollment(p) { return { orderId: 'o', farmerId: 'f', supplierId: 's', createdAt: serverTimestamp(),
  issuedAt: serverTimestamp(), salt: p.salt, verifier: p.verifier, iterations: 120000, revision: 1,
  attemptCount: 0, farmerConfirmed: false, supplierConfirmed: false }; }
async function enroll() {
  await accept(); const p = pin();
  await assertSucceeds(setDoc(doc(db('s'), 'spark_pickup_sessions/o'), enrollment(p))); return p;
}
async function handover(uid = 'f', extra = {}, eventExtra = {}) {
  const d = db(uid), b = writeBatch(d);
  b.update(doc(d, 'spark_pickup_sessions/o'), { farmerConfirmed: true, farmerConfirmedAt: serverTimestamp(), ...extra });
  score(b, d, 'f', 'farmer_handover', 3, await points('f'), eventExtra); return b.commit();
}
function collect(uid = 's') {
  const d = db(uid), b = writeBatch(d);
  b.update(doc(d, 'orders/o'), { status: 'IN_TRANSIT', pickedUpAt: serverTimestamp(), updatedAt: 2 });
  b.update(doc(d, 'spark_pickup_sessions/o'), { supplierConfirmed: true, supplierConfirmedAt: serverTimestamp() });
  score(b, d, 's', 'supplier_pickup', 4, 0); return b.commit();
}
async function deliver(uid = 's', { omitFarmer = false, omitSupplier = false } = {}) {
  const d = db(uid), b = writeBatch(d);
  const fp = (await getDoc(doc(d, 'users/f'))).get('points');
  b.update(doc(d, 'orders/o'), { status: 'DELIVERED', deliveredAt: serverTimestamp(), updatedAt: 3 });
  if (!omitFarmer) score(b, d, 'f', 'farmer_delivery', 6, fp);
  if (!omitSupplier) score(b, d, 's', 'supplier_delivery', 6, 4);
  return b.commit();
}
async function cancel(uid = 's', old = 0) {
  const d = db(uid), b = writeBatch(d);
  b.update(doc(d, 'orders/o'), { status: 'CANCELLED', cancelledAt: serverTimestamp(), cancelledBy: uid });
  b.update(doc(d, 'listings/l'), { availableQuantity: 10, soldQuantity: 0, stockOrderId: 'o' });
  score(b, d, uid, 'cancellation', -2, old); return b.commit();
}
const points = async uid => (await getDoc(doc(db('f'), 'users', uid))).get('points');

test('full request lifecycle scores Farmer 1+3+6 and Supplier 4+6; separate confirmations; reviews once', async () => {
  const p = await enroll(); assert.equal(await points('f'), 1);
  assert(timingSafeEqual(pbkdf2Sync(p.code,Buffer.from(p.salt,'hex'),120000,32,'sha256'),Buffer.from(p.verifier,'hex')));
  await assertSucceeds(handover());
  assert.equal((await getDoc(doc(db('f'), 'orders/o'))).get('status'), 'AWAITING_PICKUP');
  await assertSucceeds(collect()); await assertSucceeds(deliver());
  assert.equal(await points('f'), 10); assert.equal(await points('s'), 10);
  for (const [uid, other] of [['f','s'],['s','f']]) {
    const review = { reviewId: `o_${uid}`, orderId: 'o', reviewerId: uid, revieweeId: other, rating: 5, comment: 'Good handover' };
    await assertSucceeds(setDoc(doc(db(uid),'reviews',review.reviewId), review));
    await assertFails(setDoc(doc(db(uid),'reviews',review.reviewId), review));
  }
});
test('Supplier collection is independent and late Farmer handover after delivery is once only', async () => {
  await enroll(); await assertSucceeds(collect()); await assertSucceeds(deliver());
  assert.equal(await points('f'), 7); await assertSucceeds(handover('f',{},{}));
  assert.equal(await points('f'),10); await assertFails(handover());
});
test('Supplier enrollment stores verifier only; unrelated and Farmer create blocked', async () => {
  await accept(); const p = pin();
  for (const uid of ['f','s2','f2']) await assertFails(setDoc(doc(db(uid),'spark_pickup_sessions/o'), enrollment(p)));
  await assertSucceeds(setDoc(doc(db('s'),'spark_pickup_sessions/o'),enrollment(p)));
  const row = (await getDoc(doc(db('f'),'spark_pickup_sessions/o'))).data();
  assert(!Object.values(row).includes(p.code)); assert(!('code' in row));
  await assertFails(getDoc(doc(db('s2'),'spark_pickup_sessions/o')));
  await assertFails(getDoc(doc(db('f2'),'spark_pickup_sessions/o')));
});
test('rotation increments version, invalidates verifier and preserves attempts; no delete/reset', async () => {
  const old = await enroll();
  await assertSucceeds(updateDoc(doc(db('f'),'spark_pickup_sessions/o'), {attemptCount:1,attemptWindowAt:serverTimestamp(),lastAttemptAt:serverTimestamp()}));
  const next = pin();
  await assertSucceeds(updateDoc(doc(db('s'),'spark_pickup_sessions/o'), {salt:next.salt,verifier:next.verifier,iterations:120000,revision:2,issuedAt:serverTimestamp()}));
  assert.notEqual(old.verifier,next.verifier);
  await assertFails(updateDoc(doc(db('s'),'spark_pickup_sessions/o'), {attemptCount:0}));
  await assertFails(updateDoc(doc(db('f'),'spark_pickup_sessions/o'), {attemptCount:0}));
  await assertFails(deleteDoc(doc(db('s'),'spark_pickup_sessions/o')));
  await assertFails(deleteDoc(doc(db('f'),'spark_pickup_sessions/o')));
});
test('five persisted failed attempts lock verification even after Supplier rotation', async () => {
  await enroll(); const d = db('f');
  for(let count=1;count<=5;count++) {
    const fields={attemptCount:count,lastAttemptAt:serverTimestamp()}; if(count===1) fields.attemptWindowAt=serverTimestamp();
    await assertSucceeds(updateDoc(doc(d,'spark_pickup_sessions/o'), fields));
  }
  await assertFails(handover());
  await assertFails(updateDoc(doc(d,'spark_pickup_sessions/o'), {attemptCount:6,lastAttemptAt:serverTimestamp()}));
  const p=pin(); await assertSucceeds(updateDoc(doc(db('s'),'spark_pickup_sessions/o'), {salt:p.salt,verifier:p.verifier,revision:2,issuedAt:serverTimestamp()}));
  await assertFails(handover());
});
test('attempt window may restart only after server 30 minutes', async () => {
  await enroll(); await admin(d=>updateDoc(doc(d,'spark_pickup_sessions/o'),{attemptCount:5,attemptWindowAt:Timestamp.fromMillis(Date.now()-1801000)}));
  await assertSucceeds(updateDoc(doc(db('f'),'spark_pickup_sessions/o'), {attemptCount:1,attemptWindowAt:serverTimestamp(),lastAttemptAt:serverTimestamp()}));
});
test('server expiry rejects handover and unverified collection; rotation allows new code', async () => {
  await enroll(); await admin(d=>updateDoc(doc(d,'spark_pickup_sessions/o'),{issuedAt:Timestamp.fromMillis(Date.now()-86401000)}));
  await assertFails(handover()); await assertFails(collect());
  const p=pin(); await assertSucceeds(updateDoc(doc(db('s'),'spark_pickup_sessions/o'), {salt:p.salt,verifier:p.verifier,revision:2,issuedAt:serverTimestamp()}));
  await assertSucceeds(collect());
});
test('Rules deliberately cannot prove local PIN correctness: exact authorized attestation is accepted', async () => {
  await enroll(); await assertSucceeds(handover()); // No plaintext/hash comparison is executable in Rules.
  assert.equal(await points('f'),4);
});
for (const uid of ['s','s2','f2']) test(`${uid} cannot assert Farmer handover`,async()=>{await enroll();await assertFails(handover(uid));});
for (const uid of ['f','f2','s2']) test(`${uid} cannot collect or deliver Supplier order`,async()=>{
  await enroll();await assertFails(collect(uid));await assertSucceeds(collect());await assertFails(deliver(uid));
});
for (const fields of [{pointsDelta:99},{userId:'f2'},{role:'SUPPLIER'},{eventType:'farmer_delivery'},{createdAt:Timestamp.fromMillis(1)}])
  test(`forged event ${Object.keys(fields)[0]} is rejected atomically`,async()=>{await enroll();await assertFails(handover('f',{},fields));assert.equal(await points('f'),1);});
test('duplicate handover, pickup and delivery cannot repeat events or totals',async()=>{
  await enroll();await assertSucceeds(handover());await assertFails(handover());await assertSucceeds(collect());await assertFails(collect());
  await assertSucceeds(deliver());await assertFails(deliver());assert.equal(await points('f'),10);assert.equal(await points('s'),10);
  for(const uid of ['f','s']) { await assertFails(updateDoc(doc(db(uid),'scoreEvents/o_farmer_acceptance'),{pointsDelta:50}));
    await assertFails(deleteDoc(doc(db(uid),'scoreEvents/o_farmer_acceptance'))); }
});
test('arbitrary user points and standalone event writes are blocked',async()=>{
  await enroll();for(const uid of ['f','s','f2']) await assertFails(updateDoc(doc(db(uid),'users/f'),{points:99}));
  const d=db('f'),b=writeBatch(d);score(b,d,'f','farmer_handover',3,1);await assertFails(b.commit());
});
test('direct flags or transport status without atomic event/total cannot succeed',async()=>{
  await enroll();await assertFails(updateDoc(doc(db('f'),'spark_pickup_sessions/o'),{farmerConfirmed:true,farmerConfirmedAt:serverTimestamp()}));
  await assertFails(updateDoc(doc(db('s'),'spark_pickup_sessions/o'),{supplierConfirmed:true,supplierConfirmedAt:serverTimestamp()}));
  await assertFails(updateDoc(doc(db('s'),'orders/o'),{status:'IN_TRANSIT',pickedUpAt:serverTimestamp()}));
  await assertFails(updateDoc(doc(db('s'),'orders/o'),{status:'DELIVERED',deliveredAt:serverTimestamp()}));
});
test('delivery requires both exact reward events',async()=>{
  await enroll();await assertSucceeds(collect());await assertFails(deliver('s',{omitFarmer:true}));await assertFails(deliver('s',{omitSupplier:true}));
});
for(const uid of ['f','s']) test(`${uid} cancellation penalizes actor only, floors zero and restores stock once`,async()=>{
  await accept();await assertSucceeds(cancel(uid,uid==='f'?1:0));assert.equal(await points(uid),0);
  assert.equal(await points(uid==='f'?'s':'f'),uid==='f'?0:1);
  assert.equal((await getDoc(doc(db(uid),'listings/l'))).get('availableQuantity'),10);
  await assertFails(cancel(uid,0)); await assertFails(updateDoc(doc(db('f'),'listings/l'),{availableQuantity:15,soldQuantity:0}));
});
test('unrelated cancellation and cancellation after pickup or delivery are blocked',async()=>{
  await enroll();await assertFails(cancel('s2'));await assertSucceeds(collect());await assertFails(cancel());
  await assertSucceeds(deliver());await assertFails(cancel());
});
test('acceptance requires Farmer, stock, proposal, exact snapshots and event',async()=>{
  await assertFails(acceptBatch('s').commit());await assertFails(acceptBatch('f2').commit());
  for(const bad of [{quantity:11},{agreedPrice:99},{farmerId:'f2'},{supplierId:'s2'},{cropName:'Fake'},{status:'DELIVERED'},{pickupLocation:'Fake\n[1, 1]'}])
    await assertFails(acceptBatch('f',bad).commit());
  await assertSucceeds(acceptBatch().commit());await assertFails(acceptBatch().commit());
  assert.equal(await points('f'),1);assert.equal((await getDoc(doc(db('f'),'listings/l'))).get('soldQuantity'),5);
});
test('malicious acceptance without event and arbitrary stock rewrite fail',async()=>{
  const d=db('f'),b=writeBatch(d);b.set(doc(d,'orders/o'),order);b.update(doc(d,'order_requests/o'),{status:'ACCEPTED'});
  b.update(doc(d,'listings/l'),{availableQuantity:5,soldQuantity:5,stockOrderId:'o'});await assertFails(b.commit());
  await assertFails(updateDoc(doc(d,'listings/l'),{availableQuantity:5,soldQuantity:5}));
  await assertFails(updateDoc(doc(d,'listings/l'),{availableQuantity:-1}));
});
test('stale concurrent distinct request acceptance cannot oversell',async()=>{
  await admin(async d=>{
    await updateDoc(doc(d,'listings/l'),{quantity:5,availableQuantity:5});
    const proposal=(await getDoc(doc(d,'order_requests/o'))).data();
    await setDoc(doc(d,'order_requests/p'),{...proposal,requestId:'p'});
  });
  function attempt(id) {
    const d=db('f'),b=writeBatch(d);
    b.set(doc(d,'orders',id),{...order,orderId:id});
    b.update(doc(d,'order_requests',id),{status:'ACCEPTED'});
    b.update(doc(d,'listings/l'),{availableQuantity:0,soldQuantity:5,status:'FULFILLED',biddingEnabled:false,stockOrderId:id});
    const event=id+'_farmer_acceptance';
    b.set(doc(d,'scoreEvents',event),{orderId:id,userId:'f',role:'FARMER',eventType:'farmer_acceptance',pointsDelta:1,createdAt:serverTimestamp()});
    b.update(doc(d,'users/f'),{points:1,lastScoreEventId:event});return b.commit();
  }
  const outcomes=await Promise.allSettled([attempt('o'),attempt('p')]);
  assert.equal(outcomes.filter(r=>r.status==='fulfilled').length,1);
  assert.equal((await getDoc(doc(db('f'),'listings/l'))).get('soldQuantity'),5);
  assert.equal(await points('f'),1);
});
test('quantity, price, participant and pickup snapshot remain immutable',async()=>{
  await accept();for(const uid of ['f','s']) for(const fields of [{quantity:9},{agreedPrice:99},{supplierId:'s2'},{farmerId:'f2'},{pickupLocation:'Other\n[1, 1]'}])
    await assertFails(updateDoc(doc(db(uid),'orders/o'),fields));
  await assertSucceeds(updateDoc(doc(db('s'),'orders/o'),{deliveryLocation:'New destination\n[8, 81]',updatedAt:2}));
});
test('legacy awaiting enrollment has no acceptance backfill; terminal orders cannot enroll or gain retroactive rewards',async()=>{
  await admin(async d=>{const {sparkVersion,...legacy}=order;await setDoc(doc(d,'orders/o'),legacy);await updateDoc(doc(d,'listings/l'),{soldQuantity:5,availableQuantity:5});});
  const p=pin();await assertSucceeds(setDoc(doc(db('s'),'spark_pickup_sessions/o'),enrollment(p)));assert.equal(await points('f'),0);
});
for(const status of ['IN_TRANSIT','DELIVERED','CANCELLED']) test(`legacy ${status} cannot enroll or backfill`,async()=>{
  await admin(d=>{const {sparkVersion,...legacy}=order;return setDoc(doc(d,'orders/o'),{...legacy,status});});
  await assertFails(setDoc(doc(db('s'),'spark_pickup_sessions/o'),enrollment(pin())));
  if(status==='IN_TRANSIT') await assertSucceeds(updateDoc(doc(db('s'),'orders/o'),{status:'DELIVERED',deliveredAt:serverTimestamp()}));
  assert.equal(await points('f'),0);assert.equal(await points('s'),0);
});
test('legacy Admin confirmation or delivery receipt blocks conflicting enrollment',async()=>{
  await accept();await admin(d=>setDoc(doc(d,'order_pickup_verifications/o'),{farmerConfirmed:true,supplierConfirmed:false,code:'123456'}));
  await assertFails(setDoc(doc(db('s'),'spark_pickup_sessions/o'),enrollment(pin())));
  await assertFails(getDoc(doc(db('s'),'order_pickup_verifications/o')));
});
test('notification references restrict sender, recipient, state and payload; once only; owner read acknowledgement',async()=>{
  await accept();const d=db('f'),row={id:'ORDER_REQUEST_ACCEPTED_o',entityId:'o',eventType:'ORDER_REQUEST_ACCEPTED',actorId:'f',recipientId:'s',createdAt:serverTimestamp(),readAt:null};
  await assertFails(setDoc(doc(d,'user_notifications/s2/items',row.id),{...row,recipientId:'s2'}));
  await assertFails(setDoc(doc(d,'user_notifications/s/items',row.id),{...row,body:'Arbitrary message'}));
  await assertSucceeds(setDoc(doc(d,'user_notifications/s/items',row.id),row));
  await assertFails(setDoc(doc(d,'user_notifications/s/items',row.id),row));
  await assertFails(getDoc(doc(d,'user_notifications/s/items',row.id)));
  await assertSucceeds(updateDoc(doc(db('s'),'user_notifications/s/items',row.id),{readAt:serverTimestamp()}));
});

test('complete bid lifecycle reserves all READY stock and awards exact ten points each',async()=>{
  await admin(async d=>{
    await updateDoc(doc(d,'listings/l'),{biddingEnabled:true});
    await setDoc(doc(d,'bids/o'),{bidId:'o',listingId:'l',supplierId:'s',offerPrice:9,status:'PENDING',deliveryLocation:order.deliveryLocation});
  });
  const d=db('f'),b=writeBatch(d);
  b.set(doc(d,'orders/o'),{...order,bidId:'o',quantity:10,agreedPrice:9});
  b.update(doc(d,'bids/o'),{status:'ACCEPTED'});
  b.update(doc(d,'listings/l'),{availableQuantity:0,soldQuantity:10,status:'FULFILLED',biddingEnabled:false,stockOrderId:'o'});
  score(b,d,'f','farmer_acceptance',1,0);await assertSucceeds(b.commit());
  await assertSucceeds(setDoc(doc(db('s'),'spark_pickup_sessions/o'),enrollment(pin())));
  await assertSucceeds(handover());await assertSucceeds(collect());await assertSucceeds(deliver());
  assert.equal(await points('f'),10);assert.equal(await points('s'),10);
});
test('security-sensitive timestamps, owners, verifier schema and attempt skipping cannot be forged',async()=>{
  await enroll();
  for(const fields of [{createdAt:serverTimestamp()},{farmerId:'f2'},{supplierId:'s2'},{issuedAt:Timestamp.fromMillis(Date.now()+86400000),revision:2},
      {code:'123456'},{iterations:1},{attemptCount:4,attemptWindowAt:serverTimestamp(),lastAttemptAt:serverTimestamp()}])
    await assertFails(updateDoc(doc(db('s'),'spark_pickup_sessions/o'),fields));
  await assertFails(updateDoc(doc(db('f'),'spark_pickup_sessions/o'),{attemptCount:1,attemptWindowAt:Timestamp.fromMillis(1),lastAttemptAt:serverTimestamp()}));
});
test('reserved listing cannot be deleted or have its unit/stock ledger rewritten',async()=>{
  await accept();await assertFails(deleteDoc(doc(db('f'),'listings/l')));
  await assertFails(updateDoc(doc(db('f'),'listings/l'),{quantityUnit:'TONNE'}));
  await assertFails(updateDoc(doc(db('f'),'listings/l'),{soldQuantity:0,availableQuantity:10}));
  // Farmer-declared total crop quantity may legitimately increase, preserving existing reservations.
  await assertSucceeds(updateDoc(doc(db('f'),'listings/l'),{quantity:12,availableQuantity:7}));
});
for(const [event,status,actor,recipient,collection] of [
  ['ORDER_REQUEST_CREATED','PENDING','s','f','order_requests'],
  ['ORDER_REQUEST_REJECTED','REJECTED','f','s','order_requests'],
  ['ORDER_REQUEST_CANCELLED','CANCELLED','s','f','order_requests'],
  ['BID_CREATED','PENDING','s','f','bids'],['BID_WITHDRAWN','WITHDRAWN','s','f','bids'],['BID_REJECTED','REJECTED','f','s','bids'],
  ['BID_ACCEPTED','AWAITING_PICKUP','f','s','orders'],
  ['ORDER_IN_TRANSIT','IN_TRANSIT','s','f','orders'],['ORDER_COMPLETED','DELIVERED','s','f','orders'],['ORDER_CANCELLED','CANCELLED','f','s','orders'],
  ['HARVEST_READY','READY','f','s','harvest'],['HARVEST_FAILED','FAILED','f','s','harvest'],['HARVEST_UPDATE','EXPECTED','f','s','harvest']])
test(`Spark existing inbox supports constrained ${event} without expression-limit denial`,async()=>{
  await admin(async d=>{
    if(collection==='orders') await setDoc(doc(d,'orders/o'),{...order,status,bidId:event==='BID_ACCEPTED'?'o':'',cancelledBy:'f'});
    else if(collection==='bids') await setDoc(doc(d,'bids/o'),{bidId:'o',listingId:'l',supplierId:'s',status});
    else if(collection==='harvest') {
      await updateDoc(doc(d,'order_requests/o'),{isPreHarvest:true});await updateDoc(doc(d,'listings/l'),{harvestStatus:status});
    } else await updateDoc(doc(d,'order_requests/o'),{status});
  });
  const id=event+'_o',row={id,entityId:'o',eventType:event,actorId:actor,recipientId:recipient,createdAt:serverTimestamp(),readAt:null};
  await assertSucceeds(setDoc(doc(db(actor),`user_notifications/${recipient}/items`,id),row));
  await assertFails(setDoc(doc(db(actor),`user_notifications/${recipient==='f'?'f2':'s2'}/items`,id),{...row,recipientId:recipient==='f'?'f2':'s2'}));
});

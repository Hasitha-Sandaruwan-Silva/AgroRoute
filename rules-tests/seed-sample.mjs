import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, updateDoc } from 'firebase/firestore';

// This tool has no production mode and accepts only a loopback emulator endpoint.
const endpoint = process.env.FIRESTORE_EMULATOR_HOST || '';
if (!/^(127\.0\.0\.1|localhost):\d+$/.test(endpoint)) {
  throw new Error('Set FIRESTORE_EMULATOR_HOST to a local emulator. Production seeding is prohibited.');
}
const [host, port] = endpoint.split(':');
const environment = await initializeTestEnvironment({
  projectId: 'demo-agroroute-sample',
  firestore: { host, port: Number(port), rules: await readFile(new URL('../firestore.rules', import.meta.url), 'utf8') },
});
try {
  const sample = JSON.parse(await readFile(new URL('../sample-data/firestore.json', import.meta.url), 'utf8'));
  await environment.withSecurityRulesDisabled(async context => {
    for (const entry of sample.documents) await setDoc(doc(context.firestore(), entry.path), entry.data);
  });
  const farmer = environment.authenticatedContext('demo-farmer').firestore();
  const supplier = environment.authenticatedContext('demo-supplier').firestore();
  for (const database of [farmer, supplier]) {
    for (const path of ['users/demo-farmer', 'users/demo-supplier', 'orders/demo-order']) {
      if (!(await getDoc(doc(database, path))).exists()) throw new Error(`Missing sample ${path}`);
    }
  }
  await updateDoc(doc(farmer, 'users/demo-farmer'), { phone: '+94000000001' });
  await updateDoc(doc(supplier, 'orders/demo-order'), { deliveryLocation: 'Dummy edited warehouse\n[6.927200, 79.861300]', updatedAt: 1791504000001 });
  await updateDoc(doc(supplier, 'orders/demo-order'), { deliveryLocation: 'Dummy warehouse\n[6.927100, 79.861200]', updatedAt: 1791504000000 });
  await assertFails(updateDoc(doc(supplier, 'users/demo-supplier'), { points: 1 }));
  await assertFails(updateDoc(doc(supplier, 'orders/demo-order'), { status: 'DELIVERED' }));
  console.log(`Seeded and validated ${sample.documents.length} dummy documents in demo-agroroute-sample (local emulator only).`);
} finally { await environment.cleanup(); }

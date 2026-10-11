const { spawnSync } = require('node:child_process');
const path = require('node:path');
if (!/^127\.0\.0\.1:\d+$/.test(process.env.FIRESTORE_EMULATOR_HOST || '')) throw new Error('Local emulator required');
const root = path.resolve(__dirname, '../..');
for (const files of [
  ['functions/test/orderCompletion.emulator.cjs', 'functions/test/orderPickup.emulator.cjs', 'functions/test/orderScoring.emulator.cjs'],
  ['rules-tests/firestore.rules.test.mjs'],
  ['rules-tests/spark.lifecycle.test.mjs'],
]) {
  const result = spawnSync(process.execPath, ['--test', ...files], { cwd: root, env: process.env, stdio: 'inherit' });
  if (result.status !== 0) process.exit(result.status ?? 1);
}

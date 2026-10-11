const { test } = require('node:test');
const assert = require('node:assert/strict');
const { ORDER_SCORE, scoreAfter } = require('../lib/orderScoring');
test('central lifecycle values total ten for each role and scores cannot become negative', () => {
  assert.equal(ORDER_SCORE.farmerAccept + ORDER_SCORE.farmerHandover + ORDER_SCORE.complete, 10);
  assert.equal(ORDER_SCORE.supplierPickup + ORDER_SCORE.complete, 10);
  assert.equal(scoreAfter(1, ORDER_SCORE.cancel), 0);
  assert.equal(scoreAfter(0, ORDER_SCORE.cancel), 0);
  assert.throws(() => scoreAfter(-1, 3));
  assert.throws(() => scoreAfter(NaN, 3));
});

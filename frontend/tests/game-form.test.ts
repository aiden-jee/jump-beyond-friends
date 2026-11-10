import { test } from 'node:test';
import assert from 'node:assert/strict';
import { gameInput } from '../src/game-form.ts';

const start = new Date(Date.now() + 86400000).toISOString();
const end = new Date(Date.now() + 93600000).toISOString();
test('turns native date inputs into UTC instants and an integer threshold', () => {
  assert.deepEqual(gameInput(start, end, '8'), { startTime: start, endTime: end, threshold: 8 });
});
test('validates future times, time order, and the backend integer range', () => {
  assert.throws(() => gameInput('invalid', end, '8'), /valid start/);
  assert.throws(() => gameInput('2020-01-01T12:00', end, '8'), /future/);
  assert.throws(() => gameInput(start, start, '8'), /after/);
  assert.throws(() => gameInput(end, start, '8'), /after/);
  for (const minimum of ['1', '', '2.5', '2147483648', 'NaN']) {
    assert.throws(() => gameInput(start, end, minimum), /whole number/);
  }
});

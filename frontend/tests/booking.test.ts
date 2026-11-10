import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { confirmBooking } from '../src/booking.ts';

const originalFetch = globalThis.fetch;
afterEach(() => { globalThis.fetch = originalFetch; });
const game = { gameId: 7, status: 'PROPOSED', goingCount: 3, threshold: 8, meetsAttendanceThreshold: false };

test('fresh attendance warns below the minimum and dismissing makes no booking request', async () => {
  const calls: string[] = [];
  globalThis.fetch = async (input) => { calls.push(String(input)); return Response.json(game); };
  assert.equal(await confirmBooking(7, text => {
    assert.match(text, /Only 3 of 8/);
    assert.match(text, /minimum has not been met/);
    assert.match(text, /reserved the court separately/);
    return false;
  }), false);
  assert.deepEqual(calls, ['/api/games/7']);
});

test('accepting the warning records a booking after the fresh read', async () => {
  const calls: string[] = [];
  globalThis.fetch = async (input, init) => {
    calls.push(`${init?.method || 'GET'} ${input}`);
    return init?.method === 'POST' ? new Response(null, { status: 204 }) : Response.json(game);
  };
  assert.equal(await confirmBooking(7, () => true), true);
  assert.deepEqual(calls, ['GET /api/games/7', 'POST /api/games/7/book']);
});

test('a changed game status prevents booking before confirmation', async () => {
  globalThis.fetch = async () => Response.json({ ...game, status: 'CANCELLED' });
  await assert.rejects(confirmBooking(7, () => assert.fail('Confirmation should not open')), /no longer proposed/);
});

test('waits for the dialog decision before booking', async () => {
  const calls: string[] = [];
  let decide!: (confirmed: boolean) => void;
  let opened!: () => void;
  const dialogOpened = new Promise<void>(resolve => { opened = resolve; });
  globalThis.fetch = async (input, init) => {
    calls.push(`${init?.method || 'GET'} ${input}`);
    return init?.method === 'POST' ? new Response(null, { status: 204 }) : Response.json(game);
  };
  const booking = confirmBooking(7, () => new Promise<boolean>(resolve => {
    decide = resolve;
    opened();
  }));
  await dialogOpened;
  assert.deepEqual(calls, ['GET /api/games/7']);
  decide(true);
  assert.equal(await booking, true);
  assert.deepEqual(calls, ['GET /api/games/7', 'POST /api/games/7/book']);
});

test('dismissing the dialog leaves the game unbooked', async () => {
  const calls: string[] = [];
  globalThis.fetch = async input => { calls.push(String(input)); return Response.json(game); };
  assert.equal(await confirmBooking(7, async () => false), false);
  assert.deepEqual(calls, ['/api/games/7']);
});

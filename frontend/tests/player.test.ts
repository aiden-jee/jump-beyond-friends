import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { selectPlayer, savedPlayer, rememberPlayer } from '../src/player.ts';
import { request } from '../src/api.ts';

const originalFetch = globalThis.fetch;
afterEach(() => { globalThis.fetch = originalFetch; });
const player = { playerId: 42, name: 'Court Friend' };

test('selects an existing player without creating a duplicate', async () => {
  const calls: string[] = [];
  globalThis.fetch = async (input) => {
    calls.push(String(input));
    return Response.json(player);
  };
  assert.deepEqual(await selectPlayer(player.name), player);
  assert.deepEqual(calls, ['/api/players?name=Court%20Friend']);
});

test('creates a player only after the lookup returns 404', async () => {
  const methods: string[] = [];
  globalThis.fetch = async (_input, init) => {
    methods.push(init?.method || 'GET');
    if (methods.length === 1) return Response.json({ detail: 'Player does not exist' }, { status: 404 });
    assert.deepEqual(JSON.parse(String(init?.body)), { name: player.name });
    return Response.json(player);
  };
  assert.deepEqual(await selectPlayer(player.name), player);
  assert.deepEqual(methods, ['GET', 'POST']);
});

test('does not create a player when lookup fails with a server error', async () => {
  let calls = 0;
  globalThis.fetch = async () => { calls++; return Response.json({ detail: 'Database unavailable' }, { status: 500 }); };
  await assert.rejects(selectPlayer(player.name), /Database unavailable/);
  assert.equal(calls, 1);
});

test('recovers when another tab creates the player first', async () => {
  let calls = 0;
  globalThis.fetch = async () => {
    calls++;
    if (calls < 3) return Response.json({}, { status: calls === 1 ? 404 : 409 });
    return Response.json(player);
  };
  assert.deepEqual(await selectPlayer(player.name), player);
  assert.equal(calls, 3);
});

test('handles no-content API responses', async () => {
  globalThis.fetch = async () => new Response(null, { status: 204 });
  assert.equal(await request('/games/1/book', { method: 'POST' }), undefined);
});

test('remembers the selected ID and rejects malformed saved state', () => {
  const data = new Map<string, string>();
  Object.defineProperty(globalThis, 'localStorage', { configurable: true, value: {
    getItem: (key: string) => data.get(key) ?? null,
    setItem: (key: string, value: string) => data.set(key, value),
    removeItem: (key: string) => data.delete(key),
  } });
  assert.equal(rememberPlayer(player), true);
  assert.deepEqual(savedPlayer(), player);
  data.set('jbf.player', '{broken');
  assert.equal(savedPlayer(), null);
  data.set('jbf.player', JSON.stringify({ ...player, playerId: -1 }));
  assert.equal(savedPlayer(), null);
  rememberPlayer(null);
  assert.equal(savedPlayer(), null);
});

test('storage denial does not prevent player selection', () => {
  Object.defineProperty(globalThis, 'localStorage', { configurable: true, get: () => { throw new Error('Access denied'); } });
  assert.equal(savedPlayer(), null);
  assert.equal(rememberPlayer(player), false);
});

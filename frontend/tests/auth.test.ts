import { afterEach, test } from 'node:test';
import assert from 'node:assert/strict';
import { api, ApiError, onUnauthorized } from '../src/api.ts';
import { clearLegacyPlayer, enrollPlayer, readSession, restoreReturnPath, safeReturnPath, startGoogleSignIn } from '../src/auth.ts';

const originalFetch = globalThis.fetch;
const originalGlobals = new Map(['document', 'window', 'sessionStorage', 'localStorage'].map(key => [key, Object.getOwnPropertyDescriptor(globalThis, key)]));
const player = { id: 42, name: 'Court Friend', avatarUrl: null };

function setGlobal(key: string, value: unknown) {
  Object.defineProperty(globalThis, key, { configurable: true, value });
}
function csrfResponse() {
  setGlobal('document', { cookie: 'other=value; XSRF-TOKEN=raw-cookie-token' });
  return Response.json({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'masked-json-token' });
}
afterEach(() => {
  globalThis.fetch = originalFetch;
  for (const [key, descriptor] of originalGlobals) {
    if (descriptor) Object.defineProperty(globalThis, key, descriptor);
    else Reflect.deleteProperty(globalThis, key);
  }
});

test('GET /api/me returns the server-authenticated player', async () => {
  globalThis.fetch = async (input, init) => {
    assert.equal(input, '/api/me');
    assert.equal(init?.credentials, 'same-origin');
    assert.equal(init?.cache, 'no-store');
    assert.equal(new Headers(init?.headers).get('X-Requested-With'), 'XMLHttpRequest');
    return Response.json(player);
  };
  assert.deepEqual(await readSession(), { status: 'signed-in', player });
});

test('401 shows Google sign-in and 404 prompts private-code enrollment', async () => {
  globalThis.fetch = async () => new Response(null, { status: 401 });
  assert.deepEqual(await readSession(), { status: 'signed-out' });
  globalThis.fetch = async () => Response.json({ detail: 'Player does not exist' }, { status: 404 });
  assert.deepEqual(await readSession(), { status: 'enrollment-required' });
});

test('server and permission failures never imply a new Google user', async () => {
  for (const status of [403, 500]) {
    globalThis.fetch = async () => Response.json({ detail: 'Unavailable' }, { status });
    await assert.rejects(readSession(), error => error instanceof ApiError && error.status === status);
  }
});

test('enrollment submits only the code with the raw SPA CSRF cookie', async () => {
  const calls: string[] = [];
  globalThis.fetch = async (input, init) => {
    calls.push(String(input));
    if (input === '/api/csrf') return csrfResponse();
    assert.equal(input, '/api/me');
    assert.equal(init?.method, 'POST');
    assert.deepEqual(JSON.parse(String(init?.body)), { code: 'private code' });
    assert.equal(new Headers(init?.headers).get('X-XSRF-TOKEN'), 'raw-cookie-token');
    return Response.json(player);
  };
  assert.deepEqual(await enrollPlayer('private code'), player);
  assert.deepEqual(calls, ['/api/csrf', '/api/me']);
});

test('incorrect enrollment code reports the server message without retrying registration', async () => {
  let registrations = 0;
  globalThis.fetch = async input => {
    if (input === '/api/csrf') return csrfResponse();
    registrations++;
    return Response.json({ detail: 'Incorrect code' }, { status: 403 });
  };
  await assert.rejects(enrollPlayer('wrong'), /Incorrect code/);
  assert.equal(registrations, 1);
});

test('concurrent enrollment conflict reloads the authenticated user', async () => {
  const calls: string[] = [];
  globalThis.fetch = async (input, init) => {
    calls.push(`${init?.method || 'GET'} ${input}`);
    if (input === '/api/csrf') return csrfResponse();
    if (init?.method === 'POST') return Response.json({ detail: 'Player already exists!' }, { status: 409 });
    return Response.json(player);
  };
  assert.deepEqual(await enrollPlayer('private'), player);
  assert.deepEqual(calls, ['GET /api/csrf', 'POST /api/me', 'GET /api/me']);
});

test('missing CSRF cookie prevents writes', async () => {
  setGlobal('document', { cookie: '' });
  globalThis.fetch = async input => {
    assert.equal(input, '/api/csrf');
    return Response.json({ headerName: 'X-XSRF-TOKEN', token: 'masked-token' });
  };
  await assert.rejects(api.enroll('private'), /Allow cookies/);
});

test('RSVP creation and update use the signed-in identity and revised route', async () => {
  const writes: string[] = [];
  globalThis.fetch = async (input, init) => {
    if (input === '/api/csrf') return csrfResponse();
    writes.push(`${init?.method} ${input}`);
    assert.deepEqual(JSON.parse(String(init?.body)), { status: 'GOING' });
    assert.equal(new Headers(init?.headers).get('X-XSRF-TOKEN'), 'raw-cookie-token');
    return new Response(null, { status: init?.method === 'POST' ? 201 : 204 });
  };
  await api.createRsvp(7, 'GOING');
  await api.updateRsvp(7, 'GOING');
  assert.deepEqual(writes, ['POST /api/games/7/rsvps', 'PATCH /api/games/7/rsvps']);
});

test('expired protected requests notify the app; initial signed-out checks do not', async () => {
  let expired = 0;
  const unsubscribe = onUnauthorized(() => { expired++; });
  globalThis.fetch = async () => new Response(null, { status: 401 });
  try {
    await readSession();
    assert.equal(expired, 0);
    await assert.rejects(api.games(), /sign in again/);
    assert.equal(expired, 1);
  } finally { unsubscribe(); }
});

test('session expiry during enrollment returns the app to sign-in', async () => {
  let expired = 0;
  const unsubscribe = onUnauthorized(() => { expired++; });
  globalThis.fetch = async input => input === '/api/csrf' ? csrfResponse() : new Response(null, { status: 401 });
  try {
    await assert.rejects(enrollPlayer('private'), error => error instanceof ApiError && error.status === 401);
    assert.equal(expired, 1);
  } finally { unsubscribe(); }
});

test('logout uses CSRF and tolerates the standard HTML redirect response', async () => {
  globalThis.fetch = async (input, init) => {
    if (input === '/api/csrf') return csrfResponse();
    assert.equal(input, '/logout');
    assert.equal(init?.method, 'POST');
    assert.equal(new Headers(init?.headers).get('Accept'), 'text/html');
    assert.equal(new Headers(init?.headers).get('X-XSRF-TOKEN'), 'raw-cookie-token');
    return new Response('<html>Sign in</html>');
  };
  assert.equal(await api.logout(), undefined);
});

test('CSRF cookie changes are picked up before subsequent writes', async () => {
  let tokenNumber = 0;
  globalThis.fetch = async (input, init) => {
    if (input === '/api/csrf') {
      tokenNumber++;
      setGlobal('document', { cookie: `XSRF-TOKEN=token-${tokenNumber}` });
      return Response.json({ headerName: 'X-XSRF-TOKEN', token: 'masked' });
    }
    assert.equal(new Headers(init?.headers).get('X-XSRF-TOKEN'), `token-${tokenNumber}`);
    return new Response(null, { status: 204 });
  };
  await api.bookGame(7);
  await api.cancelGame(7);
  assert.equal(tokenNumber, 2);
});

test('return paths allow game links and reject external or API destinations', () => {
  for (const path of ['/', '/games/new', '/games/123?invite=1#rsvp']) assert.equal(safeReturnPath(path), path);
  for (const path of ['https://example.com', '//example.com', '/\\example.com', '/api/me', '/logout', 'javascript:alert(1)', null]) {
    assert.equal(safeReturnPath(path), null);
  }
});

test('Google sign-in preserves the game route and restores it on return', () => {
  const storage = new Map<string, string>();
  setGlobal('sessionStorage', {
    getItem: (key: string) => storage.get(key) ?? null,
    setItem: (key: string, value: string) => storage.set(key, value),
    removeItem: (key: string) => storage.delete(key),
  });
  let destination = '';
  let restored = '';
  const location = { pathname: '/games/123', search: '?invite=1', hash: '#rsvp', assign: (url: string) => { destination = url; } };
  setGlobal('window', { location, history: { replaceState: (_state: unknown, _title: string, url: string) => { restored = url; } } });
  startGoogleSignIn();
  assert.equal(destination, '/oauth2/authorization/google');
  location.pathname = '/'; location.search = ''; location.hash = '';
  restoreReturnPath();
  assert.equal(restored, '/games/123?invite=1#rsvp');
  assert.equal(storage.has('jbf.returnTo'), false);
});

test('old localStorage player data is discarded', () => {
  let removed = '';
  setGlobal('localStorage', { removeItem: (key: string) => { removed = key; } });
  clearLegacyPlayer();
  assert.equal(removed, 'jbf.player');
});

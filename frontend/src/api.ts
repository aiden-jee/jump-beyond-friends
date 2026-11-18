import type { Game, GameInput, Player, PlayerRsvp, RsvpStatus } from './types.ts';

export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

const unauthorizedListeners = new Set<() => void>();
export function onUnauthorized(listener: () => void): () => void {
  unauthorizedListeners.add(listener);
  return () => { unauthorizedListeners.delete(listener); };
}

async function csrfHeader(signal?: AbortSignal | null): Promise<Record<string, string>> {
  const csrf = await send<{ headerName: string }>('/api/csrf', { signal });
  // Spring's csrf.spa() expects the raw cookie in the header. The JSON token
  // returned by /api/csrf is XOR-masked and cannot be used as that header value.
  const cookie = document.cookie.split(';').map(value => value.trim()).find(value => value.startsWith('XSRF-TOKEN='));
  if (!cookie) throw new Error('Unable to prepare a secure request. Allow cookies, refresh, and try again.');
  return { [csrf.headerName]: decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) };
}

async function send<T>(path: string, init: RequestInit = {}, expectJson = true): Promise<T> {
  const method = (init.method || 'GET').toUpperCase();
  const headers = new Headers(init.headers);
  headers.set('Accept', expectJson ? 'application/json' : 'text/html');
  headers.set('X-Requested-With', 'XMLHttpRequest');
  if (init.body) headers.set('Content-Type', 'application/json');
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    for (const [name, value] of Object.entries(await csrfHeader(init.signal))) headers.set(name, value);
  }
  let response: Response;
  try {
    response = await fetch(path, {
      ...init,
      headers,
      credentials: 'same-origin',
      cache: 'no-store',
      signal: init.signal ? AbortSignal.any([init.signal, AbortSignal.timeout(15000)]) : AbortSignal.timeout(15000),
    });
  } catch (error) {
    if (init.signal?.aborted) throw error;
    throw new Error('Unable to reach the server. Check your connection and try again.');
  }
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    if (response.status === 401 && !(path === '/api/me' && method === 'GET')) unauthorizedListeners.forEach(listener => listener());
    const fallback = response.status === 401 ? 'Your session has expired. Please sign in again.'
      : response.status === 403 ? 'This request was not allowed. Refresh the page and try again.'
      : `Request failed (${response.status}). Please try again.`;
    throw new ApiError(body?.detail || body?.message || fallback, response.status);
  }
  if (!expectJson) return undefined as T;
  if (response.status === 204 || response.headers.get('content-length') === '0') return undefined as T;
  const text = await response.text();
  if (!text) return undefined as T;
  try { return JSON.parse(text) as T; }
  catch { throw new Error('The server returned an unexpected response. Refresh the page and try again.'); }
}

export function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  return send<T>(`/api${path}`, init);
}

export const api = {
  me: (signal?: AbortSignal) => request<Player>('/me', { signal }),
  enroll: (code: string) => request<Player>('/me', { method: 'POST', body: JSON.stringify({ code }) }),
  // Spring's standard logout endpoint redirects to HTML; no JSON body is expected.
  logout: () => send<void>('/logout', { method: 'POST' }, false),
  games: (signal?: AbortSignal) => request<Game[]>('/games', { signal }),
  game: (id: number, signal?: AbortSignal) => request<Game>(`/games/${id}`, { signal }),
  gameRsvps: (id: number, signal?: AbortSignal) => request<PlayerRsvp[]>(`/games/${id}/rsvps`, { signal }),
  createGame: (input: GameInput) => request<Omit<Game, 'goingCount' | 'meetsAttendanceThreshold'>>('/games', { method: 'POST', body: JSON.stringify(input) }),
  createRsvp: (gameId: number, status: RsvpStatus) => request<void>(`/games/${gameId}/rsvps`, { method: 'POST', body: JSON.stringify({ status }) }),
  updateRsvp: (gameId: number, status: RsvpStatus) => request<void>(`/games/${gameId}/rsvps`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  bookGame: (id: number) => request<void>(`/games/${id}/book`, { method: 'POST' }),
  cancelGame: (id: number) => request<void>(`/games/${id}/cancel`, { method: 'POST' }),
};

export function message(error: unknown): string {
  return error instanceof Error ? error.message : 'Something went wrong. Please try again.';
}

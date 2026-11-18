import { api, ApiError } from './api.ts';
import type { Player } from './types.ts';

export type Session =
  | { status: 'signed-out' }
  | { status: 'enrollment-required' }
  | { status: 'signed-in'; player: Player };

export async function readSession(signal?: AbortSignal): Promise<Session> {
  try { return { status: 'signed-in', player: await api.me(signal) }; }
  catch (error) {
    if (error instanceof ApiError && error.status === 401) return { status: 'signed-out' };
    if (error instanceof ApiError && error.status === 404) return { status: 'enrollment-required' };
    throw error;
  }
}

export async function enrollPlayer(code: string): Promise<Player> {
  try { return await api.enroll(code); }
  catch (error) {
    // Another tab may have completed enrollment for this Google account.
    if (error instanceof ApiError && error.status === 409) return api.me();
    throw error;
  }
}

const RETURN_PATH_KEY = 'jbf.returnTo';

export function safeReturnPath(candidate: string | null): string | null {
  if (!candidate?.startsWith('/') || candidate.startsWith('//')) return null;
  const base = 'https://jumpbeyondfriends.invalid';
  const url = new URL(candidate, base);
  if (url.origin !== base || !/^\/(?:games\/(?:new|[1-9]\d*)\/?)?$/.test(url.pathname)) return null;
  return url.pathname + url.search + url.hash;
}

export function startGoogleSignIn() {
  const path = safeReturnPath(window.location.pathname + window.location.search + window.location.hash);
  try { if (path) sessionStorage.setItem(RETURN_PATH_KEY, path); } catch { /* Sign-in still works when storage is unavailable. */ }
  window.location.assign('/oauth2/authorization/google');
}

export function restoreReturnPath() {
  let path: string | null = null;
  try {
    path = safeReturnPath(sessionStorage.getItem(RETURN_PATH_KEY));
    sessionStorage.removeItem(RETURN_PATH_KEY);
  } catch { /* Keep the current route if storage is unavailable. */ }
  if (window.location.pathname === '/' || window.location.pathname === '/login') {
    window.history.replaceState(null, '', path || '/');
  }
}

export function clearLegacyPlayer() {
  try { localStorage.removeItem('jbf.player'); } catch { /* Old player selection never establishes identity. */ }
}

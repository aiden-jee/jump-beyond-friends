import type { Game, GameInput, Player, PlayerRsvp, RsvpStatus } from './types.ts';

export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`/api${path}`, {
      ...init,
      headers: { Accept: 'application/json', ...(init.body ? { 'Content-Type': 'application/json' } : {}), ...init.headers },
      signal: init.signal ? AbortSignal.any([init.signal, AbortSignal.timeout(15000)]) : AbortSignal.timeout(15000),
    });
  } catch (error) {
    if (init.signal?.aborted) throw error;
    throw new Error('Unable to reach the server. Check your connection and try again.');
  }
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new ApiError(body?.detail || body?.message || `Request failed (${response.status}). Please try again.`, response.status);
  }
  if (response.status === 204 || response.headers.get('content-length') === '0') return undefined as T;
  const text = await response.text();
  return text ? JSON.parse(text) as T : undefined as T;
}

export const api = {
  findPlayer: (name: string) => request<Player>(`/players?name=${encodeURIComponent(name)}`),
  createPlayer: (name: string) => request<Player>('/players', { method: 'POST', body: JSON.stringify({ name }) }),
  games: (signal?: AbortSignal) => request<Game[]>('/games', { signal }),
  game: (id: number, signal?: AbortSignal) => request<Game>(`/games/${id}`, { signal }),
  gameRsvps: (id: number, signal?: AbortSignal) => request<PlayerRsvp[]>(`/games/${id}/rsvps`, { signal }),
  createGame: (input: GameInput) => request<Omit<Game, 'goingCount' | 'meetsAttendanceThreshold'>>('/games', { method: 'POST', body: JSON.stringify(input) }),
  createRsvp: (gameId: number, playerId: number, status: RsvpStatus) => request<void>(`/games/${gameId}/rsvps`, { method: 'POST', body: JSON.stringify({ playerId, status }) }),
  updateRsvp: (gameId: number, playerId: number, status: RsvpStatus) => request<void>(`/games/${gameId}/rsvps/${playerId}`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  bookGame: (id: number) => request<void>(`/games/${id}/book`, { method: 'POST' }),
  cancelGame: (id: number) => request<void>(`/games/${id}/cancel`, { method: 'POST' }),
};

export function message(error: unknown): string {
  return error instanceof Error ? error.message : 'Something went wrong. Please try again.';
}

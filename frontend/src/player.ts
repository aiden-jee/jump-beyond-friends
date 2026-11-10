import { api, ApiError } from './api.ts';
import type { Player } from './types.ts';

const STORAGE_KEY = 'jbf.player';
export function savedPlayer(): Player | null {
  try {
    const player = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
    return player && Number.isSafeInteger(player.playerId) && player.playerId > 0 && typeof player.name === 'string' && player.name.trim()
      ? player : null;
  } catch { return null; }
}
export function rememberPlayer(player: Player | null): boolean {
  try {
    if (player) localStorage.setItem(STORAGE_KEY, JSON.stringify(player));
    else localStorage.removeItem(STORAGE_KEY);
    return true;
  } catch { return false; }
}

export async function selectPlayer(name: string): Promise<Player> {
  try { return await api.findPlayer(name); }
  catch (error) {
    if (!(error instanceof ApiError) || error.status !== 404) throw error;
  }
  try { return await api.createPlayer(name); }
  catch (error) {
    // Another tab may have created the same name after our lookup.
    if (error instanceof ApiError && error.status === 409) return api.findPlayer(name);
    throw error;
  }
}

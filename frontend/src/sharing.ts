import { timeRange } from './format.ts';
import type { Game } from './types.ts';

type SharingBrowser = {
  share?: (data: ShareData) => Promise<void>;
  clipboard?: { writeText: (text: string) => Promise<void> };
};

export type ShareResult = 'shared' | 'dismissed' | 'share-failed' | 'copied' | 'manual';

export function gameInvitation(game: Game, origin: string): string | null {
  if (game.status === 'CANCELLED') return null;
  const date = new Date(game.startTime).toLocaleDateString([], {
    weekday: 'long', month: 'short', day: 'numeric', year: 'numeric',
  });
  const url = new URL(`/games/${game.gameId}`, origin).href;
  return [
    game.status === 'BOOKED' ? '🏐 Volleyball is confirmed!' : '🏐 We’re planning volleyball!',
    '',
    `📅 ${date}`,
    `🕔 ${timeRange(game.startTime, game.endTime)}`,
    `👥 ${game.goingCount} going`,
    '',
    'RSVP here:',
    url,
  ].join('\n');
}

export async function copyInvitation(text: string, browser: SharingBrowser = navigator): Promise<'copied' | 'manual'> {
  try {
    if (typeof browser.clipboard?.writeText !== 'function') return 'manual';
    await browser.clipboard.writeText(text);
    return 'copied';
  } catch {
    return 'manual';
  }
}

export async function shareInvitation(text: string, browser: SharingBrowser = navigator): Promise<ShareResult> {
  if (typeof browser.share !== 'function') return copyInvitation(text, browser);
  try {
    // Call directly from the tap handler, before any asynchronous work, for Safari.
    // The complete URL is in text; supplying url too can duplicate it in Messages.
    await browser.share({ text });
    return 'shared';
  } catch (error) {
    if (typeof error === 'object' && error !== null && 'name' in error && error.name === 'AbortError') return 'dismissed';
    // A fresh tap gives clipboard fallback its own user activation on Safari.
    return 'share-failed';
  }
}

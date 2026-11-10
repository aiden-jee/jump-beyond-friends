import { useRef, useState } from 'react';
import { gameDate, timeRange } from './format.ts';
import type { Game } from './types.ts';

export function ShareGame({ game }: { game: Game }) {
  const [feedback, setFeedback] = useState('');
  const [manual, setManual] = useState(false);
  const input = useRef<HTMLInputElement>(null);
  const url = `${window.location.origin}/games/${game.gameId}`;
  async function copy() {
    setFeedback('');
    try {
      await navigator.clipboard.writeText(url);
      setFeedback('Link copied. Paste it into your iMessage group.');
      setManual(false);
    } catch {
      setManual(true);
      setFeedback('Select and copy the link below to share it.');
    }
  }
  async function share() {
    setFeedback('');
    if (!navigator.share) { await copy(); return; }
    try {
      await navigator.share({ title: 'Jump Beyond Friends · Volleyball', url,
        text: `${gameDate(game.startTime)} · ${timeRange(game.startTime, game.endTime)}. ${game.status === 'CANCELLED' ? 'Game cancelled.' : `${game.goingCount} going / ${game.threshold} minimum. ${game.status === 'BOOKED' ? 'Booked.' : 'Proposed; reservation pending.'}`}`,
      });
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') return;
      await copy();
    }
  }
  return <section className="share-block" aria-label="Share this game"><div><h2>Same game. Whole crew.</h2><p>Send the link to your iMessage group.</p></div><div className="button-row"><button className="secondary" onClick={share}>Share ↗</button><button className="text-button" onClick={copy}>Copy link</button></div>
    {feedback && <p className="hint share-feedback" role="status">{feedback}</p>}
    {manual && <input ref={input} aria-label="Game link to copy" readOnly value={url} onFocus={() => input.current?.select()} />}
  </section>;
}

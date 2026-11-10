import { useState } from 'react';
import type { FormEvent } from 'react';
import { api, message } from './api.ts';
import { gameInput } from './game-form.ts';
import { ErrorNotice } from './ui.tsx';

export function CreateGame() {
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [minimum, setMinimum] = useState('8');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setError(''); setBusy(true);
    try {
      const game = await api.createGame(gameInput(start, end, minimum));
      window.location.assign(`/games/${game.gameId}`);
    } catch (error) { setError(message(error)); setBusy(false); }
  }
  return <section className="narrow"><a className="back-link" href="/">← All games</a><p className="eyebrow">Get the crew together</p><h1>Put a game<br />on the calendar<span className="accent">.</span></h1>
    <p className="intro">Pick a time. Set the minimum. Share it around.</p>
    <form onSubmit={submit}><fieldset disabled={busy}><legend className="sr-only">Game details</legend>
      <label htmlFor="start">Starts</label><input id="start" type="datetime-local" value={start} onChange={event => setStart(event.target.value)} required />
      <label htmlFor="end">Ends</label><input id="end" type="datetime-local" value={end} onChange={event => setEnd(event.target.value)} required />
      <p className="hint">Times are in {Intl.DateTimeFormat().resolvedOptions().timeZone.replaceAll('_', ' ')}. Friends will see their own local time.</p>
      <label htmlFor="minimum">Minimum players</label><input id="minimum" type="number" inputMode="numeric" min={2} max={2147483647} step={1} value={minimum} onChange={event => setMinimum(event.target.value)} required />
      <p className="hint">Only “Going” responses count toward the minimum. There’s no player limit.</p>
      {error && <ErrorNotice error={error} />}
      <button className="primary wide" disabled={busy}>{busy ? 'Creating game…' : 'Create game →'}</button>
    </fieldset></form><p className="privacy-note">This proposes a game. Reserve the court separately, then mark the game as booked.</p>
  </section>;
}

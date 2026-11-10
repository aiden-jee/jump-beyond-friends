import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api, ApiError, message } from './api.ts';
import { rememberPlayer, savedPlayer, selectPlayer } from './player.ts';
import { gameDate, timeRange } from './format.ts';
import type { Game, Player } from './types.ts';
import { Attendance, ErrorNotice, GameStatus } from './ui.tsx';
import { CreateGame } from './CreateGame.tsx';
import { GameDetails } from './GameDetails.tsx';

function PlayerSelection({ onSelect }: { onSelect: (player: Player) => void }) {
  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setBusy(true); setError('');
    try { onSelect(await selectPlayer(name.trim())); }
    catch (error) { setError(message(error)); }
    finally { setBusy(false); }
  }
  return <section className="welcome">
    <div className="court" aria-hidden="true"><div className="court-line" /><svg className="ball" viewBox="0 0 100 100"><circle cx="50" cy="50" r="47" fill="currentColor" /><g fill="none" stroke="#f6f5ef" strokeWidth="2"><path d="M50 3C36 19 32 37 50 50C69 52 86 69 89 77M50 50C40 67 24 73 9 73" /><path d="M64 6C48 20 47 34 58 43M78 13C65 25 60 38 64 48M3 55C20 57 35 54 41 39M4 37C20 41 30 38 37 28M31 93C38 76 51 66 66 65M53 97C55 83 64 75 78 76" /></g></svg><span className="court-label">GOOD PEOPLE. GREAT GAMES.</span></div>
    <div className="welcome-copy"><p className="eyebrow">A little less group chat</p><h1>More time<br />on court.</h1>
      <p className="intro">Find the next game. Count yourself in.<br />Bring a friend along.</p>
      <form onSubmit={submit}><label htmlFor="player-name">What’s your name?</label>
        <input id="player-name" name="name" autoComplete="name" autoCapitalize="words" placeholder="Your player name" value={name} onChange={event => setName(event.target.value)} minLength={3} maxLength={50} required disabled={busy} aria-describedby="name-help" />
        <p id="name-help" className="hint">Use your exact player name to return, or a new name to join. 3–50 characters.</p>
        {error && <ErrorNotice error={error} />}
        <button className="primary wide" disabled={busy || name.trim().length < 3}>{busy ? 'Finding your player…' : 'Let’s play →'}</button>
      </form><p className="privacy-note">No password in v1.0. Choosing a name is not secure authentication. Anyone can select an existing player.</p>
    </div>
  </section>;
}

function UpcomingGames() {
  const [games, setGames] = useState<Game[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError('');
    api.games(controller.signal).then(setGames).catch(error => {
      if (!controller.signal.aborted) setError(message(error));
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [revision]);
  return <section aria-labelledby="games-title"><div className="page-heading"><div><p className="eyebrow">Make time for a game</p><h1 id="games-title">On the calendar<span className="accent">.</span></h1></div>
    <a className="primary button-link" href="/games/new">+ New game</a></div>
    <p className="intro">A good game starts with showing up.</p>
    {loading && <p className="loading" role="status">Checking the calendar…</p>}
    {!loading && error && <ErrorNotice error={error} retry={() => setRevision(value => value + 1)} />}
    {!loading && !error && games.length === 0 && <div className="empty"><span className="empty-number">00</span><h2>A little room to play.</h2><p>No upcoming games yet. Pick a time and get the crew together.</p><a className="text-button button-link" href="/games/new">Create the first game →</a></div>}
    {!loading && !error && games.length > 0 && <div className="game-list">{games.map(game => <a className="game-card" href={`/games/${game.gameId}`} key={game.gameId}>
      <div className="date-tile"><span>{new Date(game.startTime).toLocaleDateString([], { month: 'short' })}</span><strong>{new Date(game.startTime).getDate()}</strong></div>
      <div className="game-content"><div className="game-top"><h2>{gameDate(game.startTime)}</h2><GameStatus game={game} /></div><p className="game-time">{timeRange(game.startTime, game.endTime)}</p><Attendance game={game} /><span className="game-open">View game & RSVP →</span></div>
    </a>)}</div>}
    <button className="text-button refresh-button" onClick={() => setRevision(value => value + 1)} disabled={loading}>Refresh games ↻</button>
  </section>;
}

export function App() {
  const [player, setPlayer] = useState<Player | null>(savedPlayer);
  const [restoring, setRestoring] = useState(() => savedPlayer() !== null);
  const [restoreError, setRestoreError] = useState('');
  const [storageWarning, setStorageWarning] = useState('');
  const [restoreRevision, setRestoreRevision] = useState(0);
  useEffect(() => {
    const saved = savedPlayer();
    if (!saved) { setRestoring(false); return; }
    let active = true;
    setRestoring(true); setRestoreError('');
    api.findPlayer(saved.name).then(current => {
      if (!active) return;
      if (current.playerId !== saved.playerId) {
        rememberPlayer(null); setPlayer(null);
      } else setPlayer(current);
    }).catch(error => {
      if (!active) return;
      if (error instanceof ApiError && error.status === 404) { rememberPlayer(null); setPlayer(null); }
      else setRestoreError(message(error));
    }).finally(() => { if (active) setRestoring(false); });
    return () => { active = false; };
  }, [restoreRevision]);
  function choosePlayer(next: Player | null) {
    if (!rememberPlayer(next)) setStorageWarning('Your browser could not save this player. You may need to choose your name again after reloading.');
    else setStorageWarning('');
    setPlayer(next); setRestoreError('');
  }
  return <div className="app-shell"><header className="site-header"><a className="brand" href="/" aria-label="Jump Beyond Friends home"><span className="brand-mark" aria-hidden="true">jbf<span>↗</span></span><span>JUMP BEYOND<br />FRIENDS</span></a>
    {player && <button className="player-switch" disabled={restoring} onClick={() => choosePlayer(null)}><span className="avatar" aria-hidden="true">{player.name.slice(0, 1).toUpperCase()}</span><span>{player.name}<small>Switch player</small></span></button>}
  </header><main>{storageWarning && <p className="notice" role="status">{storageWarning}</p>}
    {restoring ? <p className="loading" role="status">Getting your player ready…</p> : restoreError ? <ErrorNotice error={restoreError} retry={() => setRestoreRevision(value => value + 1)} /> : player ? <Page player={player} /> : <PlayerSelection onSelect={choosePlayer} />}
  </main><footer className="site-footer"><span>VOLLEYBALL, TOGETHER.</span><span>Jump Beyond Friends · v1.0</span></footer></div>;
}

// Three pages use ordinary URLs. Vite's SPA fallback preserves shared game links.
function Page({ player }: { player: Player }) {
  const path = window.location.pathname.replace(/\/$/, '') || '/';
  if (path === '/') return <UpcomingGames />;
  if (path === '/games/new') return <CreateGame />;
  const match = path.match(/^\/games\/([1-9]\d*)$/);
  if (match && Number.isSafeInteger(Number(match[1]))) return <GameDetails id={Number(match[1])} player={player} />;
  return <section><h1>Off the court.</h1><p>We couldn’t find that page.</p><a className="back-link" href="/">← Back to games</a></section>;
}

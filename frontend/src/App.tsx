import { useEffect, useState } from 'react';
import { api, message, onUnauthorized } from './api.ts';
import { clearLegacyPlayer, readSession, restoreReturnPath } from './auth.ts';
import type { Session } from './auth.ts';
import { AuthScreen } from './AuthScreen.tsx';
import { gameDate, timeRange } from './format.ts';
import type { Game, Player } from './types.ts';
import { Attendance, ErrorNotice, GameStatus } from './ui.tsx';
import { CreateGame } from './CreateGame.tsx';
import { GameDetails } from './GameDetails.tsx';
import { PlayerAvatar } from './PlayerAvatar.tsx';

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
  const [session, setSession] = useState<Session | null>(null);
  const [checking, setChecking] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [signingOut, setSigningOut] = useState(false);
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    clearLegacyPlayer();
    return onUnauthorized(() => {
      setSession({ status: 'signed-out' });
      setChecking(false);
      setNotice('Your session has expired. Please sign in again.');
    });
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    setChecking(true); setError('');
    readSession(controller.signal).then(next => {
      if (controller.signal.aborted) return;
      if (next.status !== 'signed-out') restoreReturnPath();
      setSession(next);
    }).catch(error => {
      if (!controller.signal.aborted) setError(message(error));
    }).finally(() => { if (!controller.signal.aborted) setChecking(false); });
    return () => controller.abort();
  }, [revision]);

  async function signOut() {
    if (signingOut) return;
    setSigningOut(true); setError('');
    try {
      await api.logout();
      setSession({ status: 'signed-out' });
      setNotice('You’ve signed out.');
    } catch (error) { setError(message(error)); }
    finally { setSigningOut(false); }
  }

  const player = session?.status === 'signed-in' ? session.player : null;
  return (
    <div className="app-shell">
      <header className="site-header">
        <a className="brand" href="/" aria-label="Jump Beyond Friends home"><span className="brand-mark" aria-hidden="true">jbf<span>↗</span></span><span>JUMP BEYOND<br />FRIENDS</span></a>
        {session && session.status !== 'signed-out' && (
          <div className="account-controls">
            {player && (
              <div className="account-identity">
                <PlayerAvatar name={player.name} avatarUrl={player.avatarUrl} />
                <span className="account-name"><small>Signed in as</small>{player.name}</span>
              </div>
            )}
            <button className="sign-out" type="button" disabled={checking || signingOut} aria-busy={signingOut} onClick={() => void signOut()}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M9 5H5v14h4M14 8l4 4-4 4M9 12h9" />
              </svg>
              {signingOut ? 'Signing out…' : 'Sign out'}
            </button>
          </div>
        )}
      </header>
      <main>
        {notice && <p className="notice" role="status">{notice}</p>}
        {error && <ErrorNotice error={error} retry={() => setRevision(value => value + 1)} />}
        {checking ? <p className="loading" role="status">Checking your sign-in…</p>
          : player ? <Page player={player} />
          : session && <AuthScreen key={session.status} needsEnrollment={session.status === 'enrollment-required'} onEnrolled={player => {
            setSession(current => current?.status === 'enrollment-required' ? { status: 'signed-in', player } : current);
            setNotice(''); setError('');
          }} />}
      </main>
      <footer className="site-footer"><span>VOLLEYBALL, TOGETHER.</span><span>Jump Beyond Friends · v1.0</span></footer>
    </div>
  );
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

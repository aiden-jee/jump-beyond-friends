import type { Game } from './types.ts';

export function ErrorNotice({ error, retry }: { error: string; retry?: () => void }) {
  return <div className="notice error" role="alert"><p>{error}</p>{retry && <button className="text-button" onClick={retry}>Try again ↻</button>}</div>;
}

export function GameStatus({ game }: { game: Game }) {
  return <span className={`status ${game.status.toLowerCase()}`}>{game.status === 'BOOKED' ? 'Booked' : game.status === 'CANCELLED' ? 'Cancelled' : 'Proposed'}</span>;
}

export function Attendance({ game }: { game: Game }) {
  const needed = Math.max(0, game.threshold - game.goingCount);
  return <div className="attendance"><div className="attendance-line"><span><strong>{game.goingCount}</strong> going <span className="muted">/ {game.threshold} minimum</span></span>
    <span className={needed ? 'muted' : 'ready'}>{needed ? `${needed} more needed` : 'Minimum met ✓'}</span></div>
    <meter min={0} max={Math.max(game.threshold, game.goingCount)} value={game.goingCount} aria-label={`${game.goingCount} going, minimum ${game.threshold}`} />
  </div>;
}

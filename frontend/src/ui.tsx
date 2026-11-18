import type { Game } from './types.ts';

export function ErrorNotice({ error, retry }: { error: string; retry?: () => void }) {
  return <div className="notice error" role="alert"><p>{error}</p>{retry && <button className="text-button" onClick={retry}>Try again ↻</button>}</div>;
}

export function GameStatus({ game }: { game: Game }) {
  return <span className={`status ${game.status.toLowerCase()}`}>{game.status === 'BOOKED' ? 'Booked' : game.status === 'CANCELLED' ? 'Cancelled' : 'Proposed'}</span>;
}

export function Attendance({ game }: { game: Game }) {
  const needed = Math.max(0, game.threshold - game.goingCount);
  const maximum = Math.max(12, game.threshold);
  return <div className="attendance"><div className="attendance-line"><span><strong>{game.goingCount}</strong> going <span className="muted">/ {game.threshold} minimum</span></span>
    <span className={needed ? 'muted' : 'ready'}>{needed ? `${needed} more needed` : 'Minimum met ✓'}</span></div>
    <div className="attendance-track">
      <meter min={0} max={maximum} value={Math.min(game.goingCount, maximum)}
        aria-label="Going attendance"
        aria-valuetext={`${game.goingCount} going, minimum ${game.threshold}; bar scale 0 to ${maximum}`} />
      {game.threshold < 12 && <span className="attendance-checkpoint" style={{ left: `${game.threshold / maximum * 100}%` }}
        title={`${game.threshold} players minimum`} aria-hidden="true" />}
    </div>
    <div className="attendance-scale" aria-hidden="true"><span>0</span><span>{maximum}</span></div>
  </div>;
}

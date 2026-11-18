import { useEffect, useRef, useState } from 'react';
import { api, message } from './api.ts';
import { gameDate, timeRange } from './format.ts';
import type { Game, Player, PlayerRsvp, RsvpStatus } from './types.ts';
import { Attendance, ErrorNotice, GameStatus } from './ui.tsx';
import { ShareGame } from './ShareGame.tsx';
import { confirmBooking } from './booking.ts';
import { ConfirmationDialog } from './ConfirmationDialog.tsx';
import { PlayerAvatar } from './PlayerAvatar.tsx';

const responses: { status: RsvpStatus; label: string }[] = [
  { status: 'GOING', label: 'Going' }, { status: 'MAYBE', label: 'Maybe' }, { status: 'NOT_GOING', label: 'Not going' },
];

export function GameDetails({ id, player }: { id: number; player: Player }) {
  const [game, setGame] = useState<Game | null>(null);
  const [rsvps, setRsvps] = useState<PlayerRsvp[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [busy, setBusy] = useState(false);
  const [showCancelDialog, setShowCancelDialog] = useState(false);
  const [bookingDescription, setBookingDescription] = useState<string | null>(null);
  const bookingDecision = useRef<((confirmed: boolean) => void) | null>(null);
  const [revision, setRevision] = useState(0);
  useEffect(() => () => { bookingDecision.current?.(false); }, []);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError('');
    Promise.all([api.game(id, controller.signal), api.gameRsvps(id, controller.signal)]).then(([nextGame, nextRsvps]) => {
      setGame(nextGame); setRsvps(nextRsvps);
    }).catch(error => { if (!controller.signal.aborted) setError(message(error)); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [id, revision]);

  const ownRsvp = rsvps.find(rsvp => rsvp.playerId === player.id);
  const rsvpName = (rsvp: PlayerRsvp) => rsvp.name || (rsvp.playerId === player.id ? player.name : `Player ${rsvp.playerId}`);
  const sortedRsvps = [...rsvps].sort((a, b) => {
    const statusOrder = responses.findIndex(response => response.status === a.status)
      - responses.findIndex(response => response.status === b.status);
    return statusOrder || rsvpName(a).localeCompare(rsvpName(b));
  });
  const unavailable = busy || loading || game?.status === 'CANCELLED';
  async function mutate(action: () => Promise<void | boolean>, success: string) {
    if (busy || loading) return;
    setBusy(true); setActionError(''); setFeedback('');
    try { if (await action() !== false) setFeedback(success); }
    catch (error) { setActionError(message(error)); }
    finally { setBusy(false); setRevision(value => value + 1); }
  }
  async function respond(status: RsvpStatus) {
    await mutate(() => ownRsvp ? api.updateRsvp(id, status) : api.createRsvp(id, status), 'Your response was saved.');
  }
  async function book() {
    await mutate(() => confirmBooking(id, text => new Promise<boolean>(resolve => {
      bookingDecision.current = resolve;
      setBookingDescription(text);
    })), 'The game was marked as booked.');
  }
  function finishBooking(confirmed: boolean) {
    const resolve = bookingDecision.current;
    bookingDecision.current = null;
    setBookingDescription(null);
    resolve?.(confirmed);
  }
  function cancel() {
    setShowCancelDialog(false);
    void mutate(() => api.cancelGame(id), 'The game was cancelled.');
  }

  return <section className="detail-page"><a className="back-link" href="/">← All games</a>
    {loading && <p className="loading" role="status">Refreshing game and responses…</p>}
    {error && <ErrorNotice error={error} retry={() => setRevision(value => value + 1)} />}
    {actionError && <ErrorNotice error={actionError} />}
    {feedback && <p className="notice" role="status">{feedback}{error ? ' Refresh to see the latest game state.' : ''}</p>}
    {game && !loading && !error && <><div className="detail-heading"><p className="eyebrow">A spot on your calendar</p><GameStatus game={game} /><h1>{gameDate(game.startTime)}</h1><p className="intro">{timeRange(game.startTime, game.endTime)}</p></div>
      <div className="detail-grid"><div><section className="detail-section"><h2>Count the crew.</h2><Attendance game={game} />
        <p className="hint">{game.status === 'CANCELLED' ? 'This game is cancelled. Attendance is shown for reference.' : game.status === 'BOOKED' ? game.meetsAttendanceThreshold ? 'The game is booked and the attendance minimum is met.' : 'The game is booked, but attendance is below the minimum. Invite a few more friends.' : game.meetsAttendanceThreshold ? 'Enough players to book. A court reservation still needs to be made.' : 'Once enough players commit, it’s time to reserve a court.'}</p>
        <div className="response-totals">{responses.map(response => <span key={response.status}><strong>{rsvps.filter(rsvp => rsvp.status === response.status).length}</strong> {response.label.toLowerCase()}</span>)}</div>
        {rsvps.length === 0 ? <p className="hint">No responses yet.</p> : (
          <ul className="player-rsvps" aria-label="Player responses">
            {sortedRsvps.map(rsvp => (
              <li key={rsvp.playerId}>
                <div className="rsvp-player">
                  <PlayerAvatar name={rsvpName(rsvp)} avatarUrl={rsvp.avatarUrl} />
                  <span className="rsvp-player-name">{rsvpName(rsvp)}{rsvp.playerId === player.id && <small className="muted"> (you)</small>}</span>
                </div>
                <span className="muted">{responses.find(response => response.status === rsvp.status)?.label}</span>
              </li>
            ))}
          </ul>
        )}
      </section><section className="detail-section"><h2>See you there, {player.name}?</h2><p className="muted">{ownRsvp ? 'Your response is saved. Plans change; you can update it here.' : 'Choose your response to let the crew know.'}</p>
        <div className="rsvp-options" role="group" aria-label="Your RSVP">{responses.map(response => <button key={response.status} className={ownRsvp?.status === response.status ? 'primary' : 'secondary'} aria-pressed={ownRsvp?.status === response.status} disabled={unavailable || ownRsvp?.status === response.status} onClick={() => void respond(response.status)}>{response.label}{ownRsvp?.status === response.status ? ' ✓' : ''}</button>)}</div>
        {busy && !bookingDescription && <p className="hint" role="status">Saving changes…</p>}
        {game.status === 'CANCELLED' && <p className="hint">Responses are closed for this cancelled game.</p>}
      </section></div><aside className={`game-actions${game.status === 'BOOKED' ? ' is-booked' : ''}`}>
        <p className="eyebrow">Game coordination</p>
        {game.status === 'BOOKED' ? (
          <><h2 className="booking-confirmed"><span className="booking-check" aria-hidden="true">✓</span>Game booked</h2>
            <p>Booking recorded. Players can still update their RSVPs.</p></>
        ) : game.status === 'CANCELLED' ? (
          <><h2>Game cancelled</h2><p>Any venue reservation must be cancelled separately.</p></>
        ) : (
          <><h2>Make it happen.</h2><p>Booking here records a court reservation you made separately.</p></>
        )}
        {game.status === 'PROPOSED' && <button className="primary wide" disabled={unavailable} onClick={() => void book()}>Mark as booked</button>}
        {game.status !== 'CANCELLED' && <button className="danger wide" disabled={unavailable} onClick={() => setShowCancelDialog(true)}>Cancel game</button>}
        <p className="hint">Any enrolled player can book or cancel a game.</p>
      </aside></div><ShareGame game={game} /><button className="text-button" onClick={() => setRevision(value => value + 1)} disabled={busy}>Refresh game ↻</button></>}
    {showCancelDialog && <ConfirmationDialog
      title="Cancel this game?"
      description="This cancels the game for everyone and closes RSVPs. Any venue reservation must be cancelled separately."
      dismissLabel="Keep game" confirmLabel="Cancel game" destructive
      onDismiss={() => setShowCancelDialog(false)} onConfirm={cancel}
    />}
    {bookingDescription !== null && <ConfirmationDialog
      title="Mark this game as booked?" description={bookingDescription}
      dismissLabel="Not yet" confirmLabel="Mark as booked"
      onDismiss={() => finishBooking(false)} onConfirm={() => finishBooking(true)}
    />}
  </section>;
}

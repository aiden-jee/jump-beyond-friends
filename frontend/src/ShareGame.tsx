import { useEffect, useId, useRef, useState } from 'react';
import { copyInvitation, gameInvitation, shareInvitation } from './sharing.ts';
import type { ShareResult } from './sharing.ts';
import type { Game } from './types.ts';

export function ShareGame({ game }: { game: Game }) {
  const [result, setResult] = useState<ShareResult | null>(null);
  const [copyOnly, setCopyOnly] = useState(() => typeof navigator.share !== 'function');
  const [busy, setBusy] = useState(false);
  const inProgress = useRef(false);
  const invitationId = useId();
  const invitation = gameInvitation(game, window.location.origin);

  useEffect(() => {
    if (result !== 'copied') return;
    const timer = window.setTimeout(() => setResult(null), 4000);
    return () => window.clearTimeout(timer);
  }, [result]);

  async function share() {
    if (!invitation || inProgress.current) return;
    inProgress.current = true;
    setBusy(true); setResult(null);
    try {
      const next = await (copyOnly ? copyInvitation(invitation) : shareInvitation(invitation));
      if (next === 'share-failed') setCopyOnly(true);
      setResult(next);
    } finally {
      inProgress.current = false;
      setBusy(false);
    }
  }

  if (!invitation) return null;

  return (
    <section className="share-block" aria-label="Share game">
      <div><h2>Same game. Whole crew.</h2><p>Invite your iMessage group with the game details and RSVP link.</p></div>
      <button className="secondary" onClick={() => void share()} disabled={busy} aria-busy={busy}>
        {copyOnly ? 'Copy Invitation' : 'Share Game'}
      </button>
      {result === 'copied' && <p className="notice share-feedback" role="status">Invitation copied!</p>}
      {result === 'share-failed' && <p className="notice share-feedback" role="status">Sharing couldn’t open. Tap “Copy Invitation” instead.</p>}
      {result === 'manual' && (
        <div className="manual-invitation">
          <p className="hint" role="status">Automatic copying isn’t available. Select the invitation below, then choose Copy.</p>
          <label htmlFor={invitationId}>Invitation to copy</label>
          <textarea id={invitationId} readOnly rows={9} value={invitation}
            onFocus={event => event.currentTarget.select()}
            onClick={event => event.currentTarget.select()} />
        </div>
      )}
    </section>
  );
}

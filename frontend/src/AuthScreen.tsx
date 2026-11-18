import { useState } from 'react';
import type { FormEvent } from 'react';
import { enrollPlayer, startGoogleSignIn } from './auth.ts';
import { message } from './api.ts';
import type { Player } from './types.ts';
import { ErrorNotice } from './ui.tsx';

export function AuthScreen({ needsEnrollment, onEnrolled }: {
  needsEnrollment: boolean;
  onEnrolled: (player: Player) => void;
}) {
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const signInFailed = window.location.pathname === '/login' && new URLSearchParams(window.location.search).has('error');

  async function enroll(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setBusy(true); setError('');
    try { onEnrolled(await enrollPlayer(code)); }
    catch (error) { setError(message(error)); }
    finally { setBusy(false); }
  }

  return (
    <section className="welcome">
      <div className="court" aria-hidden="true">
        <div className="court-line" />
        <svg className="ball" viewBox="0 0 100 100"><circle cx="50" cy="50" r="47" fill="currentColor" /><g fill="none" stroke="#f6f5ef" strokeWidth="2"><path d="M50 3C36 19 32 37 50 50C69 52 86 69 89 77M50 50C40 67 24 73 9 73" /><path d="M64 6C48 20 47 34 58 43M78 13C65 25 60 38 64 48M3 55C20 57 35 54 41 39M4 37C20 41 30 38 37 28M31 93C38 76 51 66 66 65M53 97C55 83 64 75 78 76" /></g></svg>
        <span className="court-label">GOOD PEOPLE. GREAT GAMES.</span>
      </div>
      <div className="welcome-copy">
        <p className="eyebrow">{needsEnrollment ? 'You’re almost on court' : 'A little less group chat'}</p>
        <h1>{needsEnrollment ? <>Join the<br />crew.</> : <>More time<br />on court.</>}</h1>
        <p className="intro">{needsEnrollment
          ? 'You’re signed in with Google. Enter the private code from your organizer to join.'
          : <>Find the next game. Count yourself in.<br />Bring a friend along.</>}</p>
        {needsEnrollment ? (
          <form onSubmit={enroll}>
            <label htmlFor="enrollment-code">Private code</label>
            <input id="enrollment-code" type="password" name="code" autoComplete="off" autoCapitalize="none"
              spellCheck={false} value={code} onChange={event => setCode(event.target.value)} required disabled={busy}
              aria-describedby="enrollment-help" />
            <p className="hint" id="enrollment-help">Ask your organizer for the code if you don’t have it yet.</p>
            {error && <ErrorNotice error={error} />}
            <button className="primary wide" disabled={busy || !code.trim()}>{busy ? 'Joining the crew…' : 'Join the crew →'}</button>
          </form>
        ) : (
          <>
            {signInFailed && <ErrorNotice error="Google sign-in wasn’t completed. Please try again." />}
            <button className="google-sign-in wide" onClick={startGoogleSignIn}>
              <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true"><path fill="#4285F4" d="M21.6 12.23c0-.71-.06-1.39-.18-2.05H12v3.88h5.38a4.6 4.6 0 0 1-2 3.02v2.51h3.24c1.9-1.75 2.98-4.33 2.98-7.36Z" /><path fill="#34A853" d="M12 22c2.7 0 4.96-.9 6.62-2.41l-3.24-2.51c-.9.6-2.05.96-3.38.96-2.61 0-4.82-1.76-5.61-4.12H3.05v2.59A10 10 0 0 0 12 22Z" /><path fill="#FBBC05" d="M6.39 13.92a6 6 0 0 1 0-3.84V7.49H3.05a10 10 0 0 0 0 9.02l3.34-2.59Z" /><path fill="#EA4335" d="M12 5.96c1.47 0 2.79.5 3.82 1.49l2.87-2.87A9.6 9.6 0 0 0 12 2a10 10 0 0 0-8.95 5.49l3.34 2.59C7.18 7.72 9.39 5.96 12 5.96Z" /></svg>
              Sign in with Google
            </button>
            <p className="privacy-note">New to the group? After signing in, you’ll need the private code from your organizer.</p>
          </>
        )}
      </div>
    </section>
  );
}

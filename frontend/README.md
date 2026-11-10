# Jump Beyond Friends frontend

A small React + TypeScript application built with Vite. React and React DOM are the only runtime dependencies; routing uses ordinary links and browser navigation, requests use `fetch`, and dates use native controls and `Intl`.

## Run locally

Use Node.js 22.18+ and start the existing Spring Boot/PostgreSQL backend, then:

```sh
cd frontend
npm ci
npm run dev
```

Open <http://localhost:5173>. Vite proxies `/api` to `http://localhost:8080`, so backend CORS changes are unnecessary. To use a different backend, create `frontend/.env.local` containing `BACKEND_URL=http://localhost:YOUR_PORT`, then restart Vite. The same proxy is available with `npm run preview`.

For a phone on the same network, use the Network URL printed by Vite. Native Share and clipboard APIs usually require HTTPS (localhost is an exception); the app provides a selectable link when clipboard access is unavailable. Select Messages in the native share sheet or paste the copied link into iMessage. Sharing does not send a message automatically.

```sh
npm run build
npm test
```

The build output is `dist/`. A production host must serve `index.html` for `/games/:id` and `/games/new`, and forward `/api` to Spring Boot on the same origin. Vite's development proxy is not part of the static production build.

## Scope and API contract

The controllers in `src/main/java` are the source of truth. Existing `docs/PRD.md` and `docs/TECHNICAL_DESIGN.md` describe future features and different API names.

| Method | Route | Request / response |
| --- | --- | --- |
| GET | `/api/players?name=…` | Exact, case-sensitive name → `{playerId, name}`; 404 if absent |
| POST | `/api/players` | `{name}` (3–50 characters) → `{playerId, name}` |
| GET | `/api/games` | Upcoming, non-cancelled games, ordered by start time |
| GET | `/api/games/{id}` | Game details, including cancelled games |
| POST | `/api/games` | `{startTime, endTime, threshold}` → created game (201) |
| POST | `/api/games/{id}/book` | No body; 204 |
| POST | `/api/games/{id}/cancel` | No body; 204 |
| GET | `/api/games/{id}/rsvps` | `[{playerId, status}]` |
| GET | `/api/players/{id}/rsvps` | `[{gameId, status}]` (available; not needed by these views) |
| POST | `/api/games/{id}/rsvps` | `{playerId, status}`; 201, no body |
| PATCH | `/api/games/{id}/rsvps/{playerId}` | `{status}`; 204 |

Game details: `{gameId, startTime, endTime, threshold, status, goingCount, meetsAttendanceThreshold}`. Creation returns the same shape without the attendance fields. Times are UTC ISO instants and displayed in the viewer's local timezone. Threshold is an integer of at least 2; the creation form defaults to 8. Game states are `PROPOSED`, `BOOKED`, `CANCELLED`; RSVP states are `GOING`, `MAYBE`, `NOT_GOING`. Errors use HTTP status and, where supplied, Problem Detail's `detail` field.

### Known gaps

- **No RSVP DELETE endpoint exists.** `RsvpService.deleteRsvp` is not exposed by `RsvpController`. The frontend does not invent a delete route or claim that `NOT_GOING` deletes a response. Deletion remains blocked until the backend exposes it.
- There is no player lookup by ID or player listing. The frontend stores `{playerId, name}` in localStorage under `jbf.player`, revalidates the exact name/ID on return, and allows switching players. A failed lookup only creates a new player on 404, never on a server/network error.
- **Player selection is not authentication.** Anyone can select another player's name or call the APIs directly. All selected players can create, book, and cancel games. No roles or secure access are implied.
- The backend permits below-threshold bookings. The frontend fetches fresh attendance and requires confirmation in a styled dialog, with a warning below the minimum; it cannot enforce attendance against concurrent changes. Booking/cancellation do not contact a venue.
- No venue, game editing, attendee-name lookup, or event/announcement endpoints exist. These are not fabricated in the UI. Cancelled games disappear from the upcoming list but remain accessible through their URLs.
- Counts refresh after mutations and on explicit refresh; there is no live subscription.

## Verification

Run `npm test` for the automated player-selection, validation, and booking-confirmation tests. These do not require a running backend.

Live API verification during implementation covered player creation/lookup, upcoming games, shared URLs, RSVP changes, attendance thresholds, booking, and cancellation. It left the test players `JBF Frontend Check` and `JBF Frontend Friend` and a cancelled game in PostgreSQL because no deletion endpoints exist.

Manual mobile checks: select and switch players; reload to restore selection; create a future game; open its link in a fresh tab; change RSVP statuses; dismiss and accept the below-minimum booking warning; cancel a game; test native Share and Copy Link on an HTTPS device; stop the backend to check error/retry behavior. Browser automation was unavailable during implementation.

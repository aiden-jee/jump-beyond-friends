# Jump Beyond Friends — Backend Technical Design

| Document control | Value |
|---|---|
| Version | 1.1 |
| Status | Proposed architecture; implementation in progress |
| Last updated | 2026-10-31 |
| Requirements | [Product Requirements](PRD.md) |

## Scope and implementation status

This document defines the proposed backend for the Jump Beyond Friends MVP in the JumpBeyondFriends repository. It covers persistence, attendance and booking transactions, API behavior, access control, and production operation.

The repository currently contains an initial Spring Boot application and partial domain classes. The design below describes the target implementation, not capabilities already delivered. The current entities use generated `Long` identifiers, `Game` uses `threshold` and `startTime`/`endTime`, and the Java enum uses `CANCELLED`. This design retains the proposed UUID identifiers and API terms `minPlayers`, `startAt`/`endAt`, and `CANCELED`. Resolve those differences explicitly before finalizing migrations and publishing the API contract.

## Architecture

The application is a modular monolith: a mobile web interface and REST API served from the same origin by Spring Boot, with PostgreSQL as the system of record. Google OpenID Connect provides authentication. The application manages group membership and organizer permissions locally.

```text
iMessage group
    |
    | user shares game URL
    v
Mobile browser <---- sign-in redirects ----> Google OIDC
    |
    | HTTPS: web pages and /api requests
    v
Spring Boot
    |-- Security and session handling
    |-- Membership authorization
    |-- Game and RSVP services
    |-- State-change history and announcements
    |
    v
PostgreSQL
```

The browser offers copy and native share actions. The user selects the iMessage conversation and sends the announcement. There is no server-side message delivery integration.

| Area | Technology or approach |
|---|---|
| Runtime | Java 25 |
| HTTP application | Spring Boot with Spring MVC |
| Persistence | Spring Data JPA and PostgreSQL |
| Schema changes | Flyway migrations in version control |
| Authentication | Spring Security OAuth2 client with Google OIDC |
| Sessions | Server-side HTTP sessions |
| Tests | JUnit and integration tests against PostgreSQL |
| Deployment | One application instance and PostgreSQL behind HTTPS |

Organize code by domain, with packages such as `game`, `rsvp`, `membership`, `security`, and `announcement`. Controllers handle HTTP input and output; application services enforce permissions, business rules, and transaction boundaries. Repositories handle persistence. Return API DTOs rather than exposing JPA entities directly.

The MVP does not require a message broker, distributed cache, or separately deployed domain services. Revisit these choices when operational needs justify the additional infrastructure.

## Data model

All tables below are proposed. Use versioned migrations to establish constraints and indexes; production schema changes must not depend on automatic ORM schema generation. Store timestamps as PostgreSQL `TIMESTAMPTZ` and map them to Java `Instant`.

### Players and membership

| Table | Columns and constraints |
|---|---|
| `players` | `id UUID PRIMARY KEY`; `google_sub VARCHAR NOT NULL UNIQUE`; `display_name VARCHAR NOT NULL`; `email VARCHAR NULL`; `created_at TIMESTAMPTZ NOT NULL` |
| `groups` | `id UUID PRIMARY KEY`; `name VARCHAR NOT NULL`; `created_by UUID NOT NULL REFERENCES players(id)`; `created_at TIMESTAMPTZ NOT NULL` |
| `group_memberships` | `group_id UUID NOT NULL REFERENCES groups(id)`; `player_id UUID NOT NULL REFERENCES players(id)`; `role VARCHAR NOT NULL`; `status VARCHAR NOT NULL`; primary key `(group_id, player_id)` |

Membership roles are `PLAYER` and `ORGANIZER`; membership statuses are `PENDING` and `APPROVED`. Store enums as strings and constrain accepted values in the schema. Rejection, removal, and role transfer are not defined for the MVP.

Use the validated Google OIDC subject as the external identity key. Email and display name are profile attributes and must not determine identity or permissions. This identity mapping assumes Google is the only supported issuer; adding providers requires an issuer-and-subject identity model.

### Games and RSVPs

| Table | Columns and constraints |
|---|---|
| `games` | `id UUID PRIMARY KEY`; `group_id UUID NOT NULL REFERENCES groups(id)`; `start_at TIMESTAMPTZ NOT NULL`; `end_at TIMESTAMPTZ NOT NULL`; `min_players INTEGER NOT NULL DEFAULT 8`; `booking_status VARCHAR NOT NULL`; `venue VARCHAR NULL`; `created_at TIMESTAMPTZ NOT NULL` |
| `rsvps` | `game_id UUID NOT NULL REFERENCES games(id)`; `player_id UUID NOT NULL REFERENCES players(id)`; `response VARCHAR NOT NULL`; `updated_at TIMESTAMPTZ NOT NULL`; primary key `(game_id, player_id)` |

Game constraints enforce `end_at > start_at`, `min_players >= 2`, and booking status in `PROPOSED`, `BOOKED`, or `CANCELED`. RSVP values are `GOING`, `MAYBE`, and `NOT_GOING`. The composite RSVP key guarantees one current response per player per game.

Add an index on `rsvps(game_id, response)` for attendance queries and an index on `games(group_id, start_at)` for upcoming-game lists. Membership queries by player require an index beginning with `player_id`.

`confirmedPlayers` is the count of saved `GOING` responses. Neither that count nor readiness is stored on `games`; keeping a second mutable copy would require additional consistency rules.

### State-change history

`game_events` contains:

- `id UUID PRIMARY KEY`
- `game_id UUID NOT NULL REFERENCES games(id)`
- `event_type VARCHAR NOT NULL`
- `occurred_at TIMESTAMPTZ NOT NULL`
- `actor_id UUID NULL REFERENCES players(id)`
- `details JSONB NOT NULL DEFAULT '{}'::jsonb`

Supported event types are `THRESHOLD_REACHED`, `THRESHOLD_LOST`, `GAME_BOOKED`, `BOOKED_SHORTAGE`, `BOOKED_RECOVERED`, and `GAME_CANCELED`. Event details capture the relevant before-and-after count or minimum. Index `(game_id, occurred_at)` for history retrieval.

Events are appended in the same transaction as the change they describe. They record game transitions, rather than every RSVP edit or message delivery. Retention and deletion rules for player references and history remain a production decision.

## Lifecycle and business invariants

| Current state | Command | Preconditions | Result |
|---|---|---|---|
| `PROPOSED` | Book | Approved organizer; confirmed count meets minimum | `BOOKED` and `GAME_BOOKED` event |
| `PROPOSED` or `BOOKED` | Cancel | Approved organizer | `CANCELED` and `GAME_CANCELED` event |
| `PROPOSED` or `BOOKED` | Change RSVP | Approved member; response belongs to authenticated player | Recalculate attendance and record any crossing |
| `PROPOSED` or `BOOKED` | Change minimum | Approved organizer; minimum at least 2 | Recalculate readiness or shortage and record any crossing |
| `CANCELED` | Any mutation | None permitted | Reject with a state conflict |

For proposed games, `readyToBook = confirmedPlayers >= minPlayers`. For other booking states, return `readyToBook: false`. For booked games, expose `attendanceShortage = confirmedPlayers < minPlayers`; return false for other states.

| Booking state | Crossing | Event |
|---|---|---|
| `PROPOSED` | Below minimum to meeting or exceeding it | `THRESHOLD_REACHED` |
| `PROPOSED` | Meeting minimum to below it | `THRESHOLD_LOST` |
| `BOOKED` | Meeting minimum to below it | `BOOKED_SHORTAGE` |
| `BOOKED` | Below minimum to meeting or exceeding it | `BOOKED_RECOVERED` |

No crossing means no threshold or shortage event. An unchanged RSVP is a successful no-op. Multiple crossings over time produce separate events.

Only approved members can read private game data. Organizer permissions apply to the game's group. RSVP ownership comes from the authenticated session, never from a player identifier supplied in the request body.

Booking and cancellation record an organizer's decision; they do not call a venue system. Attendance changes never undo a booking. There is no time-based cutoff, automatic cancellation, or reopening of canceled games in the MVP.

## Transactions and concurrency

Serialize attendance and lifecycle changes for each game by acquiring a pessimistic write lock on its `games` row. RSVP updates, minimum changes, booking, and cancellation must all use this lock before evaluating or modifying state. Games with different identifiers can be processed independently.

Use a service-level transaction with PostgreSQL `READ COMMITTED` isolation for this workflow. After acquiring the lock, read the attendance used for the decision. Flush pending RSVP writes before recounting so the event and returned summary reflect the saved response.

```text
updateRsvp(gameId, authenticatedPlayerId, response):
    begin transaction
    load game with write lock
    require approved membership in game's group
    require game is not canceled
    read current response and confirmed count
    if response is unchanged:
        return current game summary
    insert or update player's RSVP
    flush pending writes
    read updated confirmed count
    append event if readiness or shortage crossed the minimum
    build summary from the same transaction
    commit
```

Minimum changes compare the old and new minimum against the count obtained under the lock. Booking rechecks status and attendance under that lock, and cancellation uses it to prevent an RSVP write from completing against a canceled state.

A lock, persistence, or event-write failure rolls back the entire operation. Return success only after the transaction commits. Set bounded lock and request timeouts, and return a retryable service error when temporary database contention prevents completion. An unchanged RSVP is safe to repeat; a repeated booking request follows the current-state conflict rules.

The database key on `(game_id, player_id)` provides a separate safeguard against duplicate RSVPs. All writers must follow the locking convention, including administrative scripts.

Build game detail and list summaries from a consistent database read, such as a single aggregate query, so the displayed count, minimum, readiness, and status describe the same snapshot. The mobile client refreshes after mutations and on page reload; live updates are outside the MVP. The final booking decision always uses a fresh transactional check.

## API contract

The proposed API uses JSON under `/api`. Accept ISO 8601 timestamps with an explicit offset, convert them to `Instant`, and serialize responses in UTC. The client displays local times with a visible timezone. At creation, validate that the start is in the future and the end follows the start.

| Method | Path | Access | Behavior |
|---|---|---|---|
| GET | `/api/me` | Signed-in user | Return local player and memberships |
| GET | `/api/games` | Approved member | List upcoming games in the user's approved groups |
| GET | `/api/games/{id}` | Approved member of game's group | Return details and attendance summary |
| POST | `/api/groups/{groupId}/games` | Approved organizer of group | Create a proposed game |
| PUT | `/api/games/{id}/rsvp` | Approved member of game's group | Set authenticated player's response |
| PATCH | `/api/games/{id}/minimum` | Approved organizer of game's group | Change minimum attendance |
| POST | `/api/games/{id}/booking` | Approved organizer of game's group | Record reservation and venue |
| POST | `/api/games/{id}/cancel` | Approved organizer of game's group | Cancel game |
| GET | `/api/games/{id}/events` | Approved member of game's group | Return state-change history |
| GET | `/api/games/{id}/announcement` | Approved member of game's group | Return text for manual copy/share |
| GET | `/api/groups/{groupId}/members` | Approved organizer of group | List membership requests and members |
| POST | `/api/groups/{groupId}/members/{playerId}/approve` | Approved organizer of group | Approve membership |

The membership request and initial organizer provisioning contracts remain open. Resolve them before completing the sign-in workflow. Game history access, list pagination, and request field limits also need explicit contracts before the API is finalized.

### Create game

```json
{
  "startAt": "2026-11-07T18:00:00-08:00",
  "endAt": "2026-11-07T20:00:00-08:00",
  "minPlayers": 8
}
```

`minPlayers` is optional and defaults to 8. A successful creation returns `201 Created`, the game representation, and a `Location` header for the new resource.

### Game summary

```json
{
  "id": "8e9a12c4-6c5d-49d0-9aef-c124d8f3c910",
  "startAt": "2026-11-08T02:00:00Z",
  "endAt": "2026-11-08T04:00:00Z",
  "minPlayers": 8,
  "confirmedPlayers": 7,
  "bookingStatus": "PROPOSED",
  "readyToBook": false,
  "attendanceShortage": false,
  "venue": null
}
```

### RSVP update

```json
{ "response": "GOING" }
```

Return `200 OK` with the updated summary, including for an unchanged response. Do not accept a body field that selects another player's identity.

### Errors

Use consistent JSON errors with a stable application code, readable message, and field errors when relevant. Do not expose stack traces, SQL, or authentication credentials.

| Status | Meaning |
|---|---|
| `400` | Invalid fields, timestamps, or enum values |
| `401` | No authenticated API session |
| `403` | Authenticated user lacks permission for an otherwise visible action |
| `404` | Resource does not exist or is outside the user's approved groups |
| `409` | Current game state rejects the action, including booking below minimum |
| `503` | Temporary service or database unavailability, including lock timeout |

Browser navigation may redirect to sign-in; API calls return the applicable HTTP error. Configure this distinction explicitly in Spring Security.

## Authentication and authorization

1. A member opens a shared game URL.
2. Spring Security sends an unauthenticated browser through Google OIDC authorization-code sign-in.
3. The framework validates the authorization response and ID token, including state, issuer, audience, expiry, and nonce where used.
4. The application maps the validated subject to a local player and establishes a server-side session.
5. The browser returns to a validated same-origin path. The backend checks group membership before returning private data.

Keep CSRF protection enabled for state-changing requests, and integrate the token into the frontend. Production session cookies use `HttpOnly`, `Secure`, and an explicit `SameSite` policy compatible with the login flow. Configure session expiry and invalidate the session on logout. Restrict post-login redirects to local destinations.

Apply authorization in application services as well as route configuration. Resolve resource ownership from stored relationships; client-supplied group IDs and role fields do not establish permission.

Supply OAuth and database credentials through the deployment environment or secret store. Exclude tokens, cookies, secrets, and unnecessary profile information from logs. Configure the first organizer through a documented administrative procedure; possession of a game URL must never grant that role.

The initial deployment uses one application instance. In-memory sessions mean a restart requires users to sign in again; persisted application data remains intact. Horizontal scaling requires a shared session strategy before additional instances receive traffic.

## Announcements

Generate announcement text from authorized game data or a recorded transition. A readiness announcement describes readiness without implying that a reservation exists. Booking text reflects a saved `BOOKED` state and the available venue details.

For example:

> Saturday volleyball has 8 confirmed players and is ready to book. The reservation is still pending; the organizer will confirm the venue once it is booked.

The frontend provides Copy and, where supported, Share. It must not record delivery based on opening the device's share interface. A background worker and delivery outbox are unnecessary while all sending is manual.

## Verification

| Area | Required coverage |
|---|---|
| Domain rules | Readiness and shortage calculations, minimum validation, time validation, and permitted state transitions |
| Persistence | Foreign keys, enum constraints, RSVP uniqueness, aggregate queries, and migration application against PostgreSQL |
| Concurrency | Simultaneous RSVPs near the minimum; changes to the same player's response; minimum changes racing with RSVPs; booking racing with withdrawal; cancellation racing with mutation |
| Atomicity | Failed event persistence rolls back the triggering change; rolled-back requests leave no event |
| Security | Unauthenticated API requests, pending membership, access to another group's game, organizer restrictions, RSVP ownership, and CSRF protection |
| User workflow | Create, share, sign in, approve membership, RSVP, book, show shortage and recovery, cancel, and retain state after restart |

Concurrent tests use separate transactions and PostgreSQL connections, coordinate the competing requests, and assert the final stored state and event history. In-memory mocks do not verify database lock behavior. For a single upward crossing, assert exactly one threshold event; for distinct crossings, assert one event per transition.

## Deployment and operations

The production baseline is one application instance with PostgreSQL and HTTPS. Before launch, document the hosting environment, release process, and the person responsible for operating it.

- Separate development, test, and production configuration. Development fixtures and temporary endpoints must not be enabled in production.
- Apply and verify Flyway migrations as a controlled release step. Define application rollback and database recovery procedures for migrations that cannot be reversed safely.
- Monitor application readiness, database connectivity, request failures, latency, and lock timeouts. Protect diagnostic endpoints and exclude private details from public health responses.
- Use structured logs with request correlation identifiers. Distinguish operational logs from persisted game events.
- Configure database backups and verify restoration. Agree recovery time, acceptable data loss, and retention before production use.
- Set and test connection-pool limits, request timeouts, and session expiry for the expected group size. Availability and latency targets remain to be agreed.

These are launch requirements, not claims about the current deployment. Detailed operational procedures can be maintained separately once hosting is selected.

## Delivery plan

| Milestone | Scope | Completion criteria |
|---|---|---|
| M1 — Game persistence | Game model, migrations, validation, create/read services and endpoints | Valid games persist across restart; invalid requests fail consistently |
| M2 — Attendance | RSVP persistence, derived counts, transaction locks, threshold events | PostgreSQL concurrency tests verify counts, uniqueness, and events |
| M3 — Identity and membership | Google sign-in, player mapping, approval flow, organizer provisioning | Service and API tests enforce membership and ownership |
| M4 — Booking lifecycle | Minimum changes, reservation recording, cancellation, shortage history | Valid transitions succeed and conflicting requests preserve invariants |
| M5 — Mobile interface | Game view, RSVP and organizer actions, copy/share | Primary workflow passes on selected mobile browsers |
| M6 — Pilot and launch preparation | HTTPS deployment, operational checks, documentation, real-group trial | Pilot acceptance passes; production decisions are resolved before launch |

An initial development slice may expose `POST /api/games` and `GET /api/games/{id}` with a seeded group and organizer. Keep this creation route restricted to development and replace it with the group-scoped contract before deployment. Early slices do not satisfy the release access requirements until identity and authorization are complete.

## Trade-offs and unresolved decisions

| Topic | Current position | Follow-up |
|---|---|---|
| Manual sharing | Members learn of changes when they revisit the page or someone shares an update | Assess notification needs after the pilot |
| Per-game write lock | Simplifies attendance and transition consistency for a small group | Measure contention before changing the strategy |
| Derived attendance | Avoids maintaining a second mutable count | Review query performance with actual data |
| Identifiers and naming | Proposed UUID schema and API differ from the current Java model | Resolve before migrations and API contract are finalized |
| Membership onboarding | Approval is required; request and bootstrap flows are incomplete | Define before pilot |
| Venue and announcement storage | Basic venue string; announcement draft persistence undecided | Confirm required fields and generation behavior before workflow acceptance |
| History and retention | Events are retained; no archival or deletion workflow is defined | Define authorized history access and production retention policy |
| Operations | Hosting and service objectives are not yet selected | Agree ownership, monitoring, backup, and recovery expectations before launch |

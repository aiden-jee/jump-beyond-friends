# Jump Beyond Friends — Product Requirements

| Document control | Value |
|---|---|
| Version | 1.1 |
| Status | Proposed MVP requirements; implementation in progress |
| Audience | Product, engineering, and operations contributors |
| Last updated | 2026-10-31 |
| Technical design | [Backend Technical Design](TECHNICAL_DESIGN.md) |

## Purpose

Jump Beyond Friends helps a private volleyball group confirm attendance before an organizer rents a gym. The organizer posts a game link in the existing iMessage group, members record their availability, and the application shows whether enough players have committed.

The organizer reserves the gym outside the application and records the booking afterward. Announcements are prepared in the application and shared manually to iMessage.

### Problem

The group usually plays once a week at a fixed time. Renting a gym depends on having enough committed players, but responses in group chat are difficult to track as people change their plans. Organizers need a reliable attendance record and a clear distinction between having enough players and having a confirmed reservation.

### Intended outcome

An organizer can coordinate a game through one shared link, see the current attendance, record a reservation, and communicate changes without moving the group to another messaging platform.

The pilot is successful when a real group completes this workflow on mobile devices, attendance remains accurate through response changes, and the booking record remains consistent with the organizer's actions. Numerical adoption and performance targets have not yet been agreed.

## Users and access

| User | Capabilities |
|---|---|
| Player | Sign in with Google, request group membership, view games for approved groups, and submit or change their own RSVP. |
| Organizer | All player capabilities, plus approve membership, create games, manage minimum attendance and venue details, record bookings, and cancel games. |

Google sign-in establishes identity. Group membership determines access to private game information, and the organizer role permits administrative actions within that group. A shared game URL does not grant access on its own.

Membership requires organizer approval. Initial organizer setup and the membership request flow must be defined before the pilot; see [Decisions still required](#decisions-still-required).

## Scope and functional requirements

The MVP supports manually created games for a private group. The group may play regularly, but each game is created individually.

| ID | Requirement | Acceptance criteria |
|---|---|---|
| FR-01 | Create a game | An approved organizer can create a game with a future start time and an end time after the start. The game starts as `PROPOSED` with zero confirmed players. |
| FR-02 | Set minimum attendance | The minimum defaults to 8 and must be at least 2. An organizer can change it for a proposed or booked game; canceled games cannot be changed. |
| FR-03 | Share a game | The organizer can copy a game URL or open the device's share interface, where supported. The URL opens the same game for each authorized member. |
| FR-04 | Submit an RSVP | An approved member can select `GOING`, `MAYBE`, or `NOT_GOING`. Only `GOING` contributes to confirmed attendance. |
| FR-05 | Change an RSVP | A member can change their own response for a proposed or booked game. Each player has one current response per game; repeated submissions do not create duplicates. |
| FR-06 | Accept all confirmed players | There is no maximum attendance or waitlist in the MVP. |
| FR-07 | Show booking readiness | A proposed game is ready to book when confirmed attendance meets or exceeds its current minimum. The page displays the count, minimum, and booking status. |
| FR-08 | Record readiness changes | For a proposed game, crossing the minimum in either direction records a state-change event. This applies to RSVP changes and changes to the minimum. |
| FR-09 | Record a reservation | An organizer can mark a proposed game as `BOOKED` when it meets the minimum, and record venue details. The reservation is made outside the application. |
| FR-10 | Show attendance shortages | RSVPs remain editable after booking. If attendance falls below the minimum, the game remains `BOOKED` and displays a shortage. Recovery clears that indication. |
| FR-11 | Cancel a game | An organizer can cancel a proposed or booked game. A canceled game remains visible to authorized members but rejects further changes. |
| FR-12 | Prepare announcements | Members can copy or share text describing readiness, loss of readiness, booking, a booked-game shortage or recovery, and cancellation. Sending the message requires a user action. |
| FR-13 | Control membership | An organizer can approve membership requests for their group. Only approved members can view private game details or submit RSVPs. |
| FR-14 | Keep decisions manual | There is no automatic booking deadline, RSVP cutoff, or cancellation. The organizer controls booking and cancellation. |

## Business rules

### Attendance

A player's current RSVP is unique within a game. `GOING` contributes one confirmed player; `MAYBE`, `NOT_GOING`, and no response contribute zero. Confirmed attendance is calculated from saved responses.

For a proposed game, booking readiness is calculated as:

```text
readyToBook = confirmedPlayers >= minPlayers
```

Changing the minimum triggers the same evaluation as changing an RSVP. A game can cross the threshold more than once as attendance changes.

### Booking lifecycle

| Current status | Action | Result |
|---|---|---|
| `PROPOSED` | Organizer records a reservation and attendance meets the minimum | `BOOKED` |
| `PROPOSED` | Organizer cancels | `CANCELED` |
| `BOOKED` | Organizer cancels | `CANCELED` |
| `BOOKED` | Attendance or minimum changes | Remains `BOOKED`; shortage indication is recalculated |
| `CANCELED` | Attempt to change the game or an RSVP | Request is rejected |

Readiness is separate from booking status. Meeting the minimum does not reserve a gym, and losing players does not reverse a reservation. Cancellation closes the record in the application; any cancellation with the venue is handled by the organizer outside the application. Reopening canceled games is outside the MVP.

Games have no automatic time-based transition. In the MVP, a proposed or booked game remains editable until canceled, including after its scheduled end. A later release may introduce archival or a response cutoff.

### State-change history and announcements

For proposed games, record `THRESHOLD_REACHED` when attendance moves from below the minimum to meeting it, and `THRESHOLD_LOST` for the reverse. For booked games, record `BOOKED_SHORTAGE` when attendance falls below the minimum and `BOOKED_RECOVERED` when it meets the minimum again. Booking and cancellation also create events.

Saving the same RSVP again does not create a state-change event. A later crossing of the same threshold is a separate event. These events describe game transitions; they are not a complete audit of every RSVP edit.

Announcement text uses the saved game state. Opening a share interface does not confirm message delivery, and the application does not report an announcement as sent on that basis.

## Primary workflow

1. The organizer creates a Saturday game from 6–8 PM with a minimum of eight players.
2. The organizer posts the game link in iMessage.
3. Members sign in with Google. New members request access and wait for approval before viewing private details or responding.
4. Members submit RSVPs. The game page shows the current confirmed count and minimum.
5. The eighth confirmed player makes the proposed game ready to book. The application records the threshold crossing.
6. The organizer reserves the gym, records the booking and venue, and shares the prepared booking announcement.
7. If a player withdraws, the game stays booked. A shortage appears if attendance falls below eight.
8. The organizer recruits another player, shares an update, or cancels the game as appropriate.

## Quality and operational requirements

| ID | Requirement | Release evidence |
|---|---|---|
| NFR-01 | Mobile usability | Sign-in, membership approval, RSVP changes, booking, and copy/share actions work on the mobile browsers selected for the pilot. |
| NFR-02 | Access control | Server-side checks enforce membership, organizer permissions, and RSVP ownership. Tests cover unauthenticated and unauthorized requests, including access through a shared URL. |
| NFR-03 | Data consistency | Concurrent RSVP, minimum, booking, and cancellation requests preserve attendance totals and valid state transitions. PostgreSQL integration tests verify the behavior. |
| NFR-04 | Durability | Games, RSVPs, memberships, and events survive application restarts. Database backup and restore procedures are documented and verified before production use. |
| NFR-05 | Secure operation | Production uses HTTPS and secure sessions. Credentials are supplied through deployment configuration and excluded from source control and logs. |
| NFR-06 | Supportability | Validation and access failures have consistent responses. Operational logs and health checks support diagnosis without exposing private group information or credentials. |
| NFR-07 | Repeatable delivery | Local setup, versioned schema migrations, deployment, and recovery procedures are documented. |

The backend uses Java and Spring Boot with PostgreSQL. Implementation details belong in the [technical design](TECHNICAL_DESIGN.md).

Availability, response-time targets, expected load, and recovery objectives must be agreed before production launch. These targets remain open rather than implying service levels that have not been measured or approved.

## Release acceptance

The MVP is ready for a pilot when the following checks pass:

1. An organizer creates a game with the default minimum of eight. Invalid dates and minimum values are rejected.
2. Eight approved members submit `GOING`, including simultaneous requests. The final saved count is eight, with one current RSVP per member and one `THRESHOLD_REACHED` event for the crossing.
3. Repeating an unchanged RSVP leaves the count and event history unchanged. Changing the minimum produces the expected readiness transition.
4. The organizer records a reservation. A subsequent withdrawal produces a shortage while the game remains `BOOKED`; a replacement player clears the shortage.
5. A member can change only their own RSVP. A pending member cannot view private game details, and a player cannot perform organizer actions.
6. Cancellation prevents further game and RSVP changes. The record remains available to authorized members.
7. Copy/share actions produce accurate announcement text without claiming delivery.
8. Saved state survives an application restart, and the primary workflow passes on the selected mobile browsers.

Production launch also requires the evidence in [Quality and operational requirements](#quality-and-operational-requirements) and resolution of the production decisions in [Decisions still required](#decisions-still-required).

## Out of scope

- Gym reservations, payments, or venue cancellation through external systems.
- Automatic iMessage delivery, SMS bots, and Messages automation.
- Booking deadlines, automatic cancellation, and recurring-game generation.
- Alternative time-slot polling and calendar availability matching.
- Waitlists, capacity limits, player ratings, and team balancing.
- Calendar integrations, native mobile applications, and advanced analytics.

## Decisions still required

| Decision | Required by |
|---|---|
| Initial organizer provisioning and membership request flow | Pilot |
| Mobile browser support and frontend approach | Pilot |
| Required venue fields and their validation | Booking workflow acceptance |
| Announcement generation on demand or saved drafts | Announcement workflow acceptance |
| Hosting, service targets, monitoring ownership, backup and recovery objectives | Production launch |
| Retention and deletion policy for player information and event history | Production launch |

Automated notification channels may be considered after the pilot. They are not a dependency for the MVP.

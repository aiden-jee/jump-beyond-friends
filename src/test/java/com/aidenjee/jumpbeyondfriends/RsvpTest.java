package com.aidenjee.jumpbeyondfriends;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

public class RsvpTest {
    private Player createPlayer(Long id) {
        Player player = new Player("Aiden");
        ReflectionTestUtils.setField(player, "id", id);
        return player;
    }

    private Game createGame(Long id) {
        Instant start = Instant.parse("2100-01-01T18:00:00Z");
        Game game = new Game(start, start.plus(2, ChronoUnit.HOURS));
        ReflectionTestUtils.setField(game, "id", id);
        return game;
    }

    @Test
    void newRsvpShouldUsePlayerAndGameIds() {
        Player player = createPlayer(7L);
        Game game = createGame(42L);

        Rsvp rsvp = new Rsvp(player, game, RsvpStatus.GOING);

        assertEquals(7L, rsvp.getId().getPlayerId());
        assertEquals(42L, rsvp.getId().getGameId());
        assertEquals(new RsvpId(7L, 42L), rsvp.getId());
        assertSame(player, ReflectionTestUtils.getField(rsvp, "player"));
        assertSame(game, ReflectionTestUtils.getField(rsvp, "game"));
    }

    // Rsvp does not expose its status through a getter.
    @ParameterizedTest
    @EnumSource(RsvpStatus.class)
    void newRsvpShouldRetainInitialStatus(RsvpStatus status) {
        Rsvp rsvp = new Rsvp(createPlayer(7L), createGame(42L), status);

        assertEquals(status, ReflectionTestUtils.getField(rsvp, "status"));
    }

    @ParameterizedTest
    @EnumSource(RsvpStatus.class)
    void changeStatusShouldUpdateStatusAndPreserveIdentity(RsvpStatus status) {
        RsvpStatus initialStatus = status == RsvpStatus.GOING
            ? RsvpStatus.NOT_GOING : RsvpStatus.GOING;
        Player player = createPlayer(7L);
        Game game = createGame(42L);
        Rsvp rsvp = new Rsvp(player, game, initialStatus);
        RsvpId originalId = rsvp.getId();

        rsvp.changeStatus(status);

        assertEquals(status, rsvp.getStatus());
        assertSame(originalId, rsvp.getId());
        assertEquals(new RsvpId(7L, 42L), rsvp.getId());
        assertSame(player, ReflectionTestUtils.getField(rsvp, "player"));
        assertSame(game, ReflectionTestUtils.getField(rsvp, "game"));
    }

    @ParameterizedTest
    @EnumSource(RsvpStatus.class)
    void changeStatusShouldAllowCurrentStatus(RsvpStatus status) {
        Rsvp rsvp = new Rsvp(createPlayer(7L), createGame(42L), status);

        rsvp.changeStatus(status);

        assertEquals(status, rsvp.getStatus());
    }
}

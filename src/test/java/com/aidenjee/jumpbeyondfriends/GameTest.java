package com.aidenjee.jumpbeyondfriends;

import com.aidenjee.jumpbeyondfriends.game.AppConstants;
import com.aidenjee.jumpbeyondfriends.game.Game;
import com.aidenjee.jumpbeyondfriends.game.GameStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class GameTest {
    private Game createValidGame() {
        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        return new Game(start, end);
    }

    @Test
    void newGameShouldHaveDefaultThreshold() {
        Game game = createValidGame();

        assertEquals(
            AppConstants.DEFAULT_THRESHOLD, 
            game.getThreshold()
        );
    }

    @Test 
    void newGameShouldHaveProposedStatus() {
        Game game = createValidGame();

        assertEquals(
            GameStatus.PROPOSED, 
            game.getStatus()
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {-10, -1, 0, 1})
    void newGameShouldRejectInvalidThresholds(int threshold) {
        Game game = createValidGame();

        assertThrows(
            IllegalArgumentException.class,
            () -> game.setThreshold(threshold)
        );
    }

    @Test 
    void newGameShouldRejectInvalidTimes() {
        Instant before = Instant.now().plus(7, ChronoUnit.DAYS);
        Instant after = before.plus(2, ChronoUnit.HOURS);

        assertThrows(
            IllegalArgumentException.class, 
            () -> new Game(before, before)
        );
        assertThrows(
            IllegalArgumentException.class, 
            () -> new Game(after, before)
        );
    }
    @Test 
    void rescheduleShouldRejectInvalidTimes() {
        Game game = createValidGame();
        Instant before = Instant.now().plus(7, ChronoUnit.DAYS);
        Instant after = before.plus(2, ChronoUnit.HOURS);

        assertThrows(
            IllegalArgumentException.class, 
            () -> game.reschedule(before, before)
        );
        assertThrows(
            IllegalArgumentException.class, 
            () -> game.reschedule(after, before)
        );
    }

}

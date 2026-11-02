package com.aidenjee.jumpbeyondfriends;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class PlayerTest {
    @ParameterizedTest
    @ValueSource(strings = {"A", "Aiden", "  Aiden  ", "민지"})
    void newPlayerShouldPreserveValidName(String name) {
        Player player = new Player(name);

        assertEquals(name, player.getName());
    }

    @Test
    void newPlayerShouldAcceptNameAtMaximumLength() {
        String name = "A".repeat(50);

        Player player = new Player(name);

        assertEquals(name, player.getName());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n", " \t\n "})
    void newPlayerShouldRejectBlankName(String name) {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Player(name)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {51, 100})
    void newPlayerShouldRejectNameExceedingMaximumLength(int length) {
        String name = "A".repeat(length);

        assertThrows(
            IllegalArgumentException.class,
            () -> new Player(name)
        );
    }

    @Test
    void newPlayerShouldHaveNoIdBeforePersistence() {
        Player player = new Player("Aiden");

        assertNull(player.getId());
    }
}

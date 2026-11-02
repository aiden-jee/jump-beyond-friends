package com.aidenjee.jumpbeyondfriends;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class RsvpServiceTest {
    @Mock 
    private RsvpRepository rsvpRepository;

    @Mock 
    private GameRepository gameRepository;

    @Mock 
    private PlayerRepository playerRepository;

    @InjectMocks 
    private RsvpService rsvpService;

    private Game createGame(Long id) {
        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        Game game = new Game(start, start.plus(2, ChronoUnit.HOURS));
        ReflectionTestUtils.setField(game, "id", id);
        return game;
    }

    private Player createPlayer(String name, Long id) {
        Player player = new Player(name);
        ReflectionTestUtils.setField(player, "id", id);
        return player;
    }

    @Test
    public void createRsvpShouldSaveRsvpWithCorrectPlayerGameAndStatus() {
        Player player = createPlayer("RSVP Test", 1L);
        when(playerRepository.findById(1L))
            .thenReturn(Optional.of(player));

        Game game = createGame(2L);
        when(gameRepository.findById(2L))
            .thenReturn(Optional.of(game));

        when(rsvpRepository.existsById(new RsvpId(1L, 2L)))
            .thenReturn(false);

        rsvpService.createRsvp(1L, 2L, RsvpStatus.GOING);

        ArgumentCaptor<Rsvp> captor = ArgumentCaptor.forClass(Rsvp.class);
        verify(rsvpRepository).save(captor.capture());
        Rsvp savedRsvp = captor.getValue();
        assertEquals(
            new RsvpId(1L, 2L),
            savedRsvp.getId()
        );
        assertEquals(
            RsvpStatus.GOING,
            savedRsvp.getStatus()
        );
    }

    @Test public void duplicateRsvpShouldThrowException() {
        when(rsvpRepository.existsById(new RsvpId(1L, 2L)))
            .thenReturn(true);
        assertThrows(
            IllegalStateException.class,
            () -> rsvpService.createRsvp(1L, 2L, RsvpStatus.GOING)
        );
        verify(rsvpRepository, never()).save(any(Rsvp.class));
    }

    @Test 
    public void invalidPlayerShouldThrowException() {
        when(playerRepository.findById(1L)).thenReturn(Optional.empty());
        when(rsvpRepository.existsById(new RsvpId(1L, 2L)))
            .thenReturn(false);
        NoSuchElementException e = assertThrows(
            NoSuchElementException.class,
            () -> rsvpService.createRsvp(1L, 2L, RsvpStatus.GOING)
        );
        assertEquals("Player does not exist!", e.getMessage());
        verify(rsvpRepository, never()).save(any(Rsvp.class));
    }

    @Test 
    public void invalidGameShouldThrowException() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(createPlayer("Aiden", 1L)));
        when(gameRepository.findById(2L)).thenReturn(Optional.empty());

        when(rsvpRepository.existsById(new RsvpId(1L, 2L)))
            .thenReturn(false);
        NoSuchElementException e = assertThrows(
            NoSuchElementException.class,
            () -> rsvpService.createRsvp(1L, 2L, RsvpStatus.GOING)
        );
        assertEquals("Game does not exist!", e.getMessage());
        verify(rsvpRepository, never()).save(any(Rsvp.class));
    }
}

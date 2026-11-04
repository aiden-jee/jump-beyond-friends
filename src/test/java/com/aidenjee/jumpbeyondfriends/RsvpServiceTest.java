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

import com.aidenjee.jumpbeyondfriends.game.Game;
import com.aidenjee.jumpbeyondfriends.game.GameRepository;
import com.aidenjee.jumpbeyondfriends.game.GameStatus;
import com.aidenjee.jumpbeyondfriends.player.Player;
import com.aidenjee.jumpbeyondfriends.player.PlayerRepository;
import com.aidenjee.jumpbeyondfriends.rsvp.Rsvp;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpId;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpRepository;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpService;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpStatus;

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
        when(gameRepository.findByIdForWrite(2L))
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

    @Test public void createDuplicateRsvpShouldThrowException() {
        when(rsvpRepository.existsById(new RsvpId(1L, 2L)))
            .thenReturn(true);
        assertThrows(
            IllegalStateException.class,
            () -> rsvpService.createRsvp(1L, 2L, RsvpStatus.GOING)
        );
        verify(rsvpRepository, never()).save(any(Rsvp.class));
    }

    @Test 
    public void createRsvpInvalidPlayerShouldThrowException() {
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
    public void createRsvpInvalidGameShouldThrowException() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(createPlayer("Aiden", 1L)));
        when(gameRepository.findByIdForWrite(2L)).thenReturn(Optional.empty());

        when(rsvpRepository.existsById(new RsvpId(1L, 2L)))
            .thenReturn(false);
        NoSuchElementException e = assertThrows(
            NoSuchElementException.class,
            () -> rsvpService.createRsvp(1L, 2L, RsvpStatus.GOING)
        );
        assertEquals("Game does not exist!", e.getMessage());
        verify(rsvpRepository, never()).save(any(Rsvp.class));
    }

    @Test 
    public void updateRsvpShouldUpdateRsvpWithCorrectPlayerGameAndStatus() {
        Player player = createPlayer("RSVP Test", 1L);

        Game game = createGame(2L);
        when(gameRepository.findByIdForWrite(2L))
            .thenReturn(Optional.of(game));

        Rsvp rsvp = new Rsvp(player, game, RsvpStatus.GOING);
        when(rsvpRepository.findById(new RsvpId(1L, 2L)))
            .thenReturn(Optional.of(rsvp));
        
        rsvpService.updateRsvpStatus(1L, 2L, RsvpStatus.NOT_GOING);
        assertEquals(RsvpStatus.NOT_GOING, rsvp.getStatus());
        assertEquals(new RsvpId(1L, 2L), rsvp.getId());
    }

    @Test
    public void updateRsvpInvalidGameShouldThrowException() {
        Player player = createPlayer("RSVP Test", 1L);

        Game game = createGame(2L);
        when(gameRepository.findByIdForWrite(2L))
            .thenReturn(Optional.empty());

        Rsvp rsvp = new Rsvp(player, game, RsvpStatus.GOING);
        when(rsvpRepository.findById(new RsvpId(1L, 2L)))
            .thenReturn(Optional.of(rsvp));
        
        NoSuchElementException e = assertThrows(
            NoSuchElementException.class, 
            () -> rsvpService.updateRsvpStatus(1L, 2L, RsvpStatus.NOT_GOING));
        assertEquals("Game does not exist!", e.getMessage());
        assertEquals(RsvpStatus.GOING, rsvp.getStatus());
    }

    @Test 
    public void cannotUpdateRsvpCancelledGame() {
        Player player = createPlayer("RSVP Test", 1L);

        Game game = createGame(2L);
        when(gameRepository.findByIdForWrite(2L))
            .thenReturn(Optional.of(game));

        Rsvp rsvp = new Rsvp(player, game, RsvpStatus.GOING);
        when(rsvpRepository.findById(new RsvpId(1L, 2L)))
            .thenReturn(Optional.of(rsvp));
        
        game.setStatus(GameStatus.CANCELLED);
        
        IllegalStateException e = assertThrows(
            IllegalStateException.class, 
            () -> rsvpService.updateRsvpStatus(1L, 2L, RsvpStatus.NOT_GOING));
        assertEquals("Cannot RSVP to a cancelled game", e.getMessage());
        assertEquals(RsvpStatus.GOING, rsvp.getStatus());
    }

    @Test 
    public void cannotUpdateNonExistantRsvp() {
        when(rsvpRepository.findById(new RsvpId(1L, 2L)))
            .thenReturn(Optional.empty());
        
        NoSuchElementException e = assertThrows(
            NoSuchElementException.class, 
            () -> rsvpService.updateRsvpStatus(1L, 2L, RsvpStatus.NOT_GOING));
        assertEquals("RSVP does not exist!", e.getMessage());
    }

    @Test
    void deleteExistingRsvpShouldCallRepositoryDelete() {
        Player player = createPlayer("RSVP Test", 1L);
        Game game = createGame(2L);
        Rsvp rsvp = new Rsvp(player, game, RsvpStatus.GOING);

        when(gameRepository.findByIdForWrite(2L))
                    .thenReturn(Optional.of(game));
        when(rsvpRepository.findById(new RsvpId(1L, 2L)))
            .thenReturn(Optional.of(rsvp));
        rsvpService.deleteRsvp(1L, 2L);
        verify(rsvpRepository).delete(rsvp);
    }

    @Test 
    public void updateNonexistentRsvpShouldThrowException() {
        Game game = createGame(2L);
        when(gameRepository.findByIdForWrite(2L))
                    .thenReturn(Optional.of(game));
        when(rsvpRepository.findById(new RsvpId(1L, 2L)))
            .thenReturn(Optional.empty());
        
        NoSuchElementException e = assertThrows(
            NoSuchElementException.class, 
            () -> rsvpService.deleteRsvp(1L, 2L));
        assertEquals("RSVP does not exist!", e.getMessage());
        verify(rsvpRepository, never()).delete(any(Rsvp.class));
    }
}

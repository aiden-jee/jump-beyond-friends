package com.aidenjee.jumpbeyondfriends;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class RsvpService {

    private final RsvpRepository rsvpRepository;
    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;

    public RsvpService(
            RsvpRepository rsvpRepository,
            PlayerRepository playerRepository,
            GameRepository gameRepository) {
        this.rsvpRepository = rsvpRepository;
        this.playerRepository = playerRepository;
        this.gameRepository = gameRepository;
    }

    // methods
    /**
     * Create Rsvp for the specified player id and game id with the given status. 
     * Throws an exception when the Rsvp already exists.
     */
    public void createRsvp(Long playerId, Long gameId, RsvpStatus status) {
        if (rsvpRepository.existsById(new RsvpId(playerId, gameId))) {
            throw new IllegalStateException("RSVP already exists");
        }

        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new NoSuchElementException("Player does not exist!"));
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new NoSuchElementException("Game does not exist!"));

        if (game.getStatus() == GameStatus.CANCELLED) {
            throw new IllegalStateException("Cannot RSVP to a canceled game");
        }

        Rsvp rsvp = new Rsvp(player, game, status);
        rsvpRepository.save(rsvp);
    }

    public void updateRsvpStatus(Long playerId, Long gameId, RsvpStatus status) {
        Rsvp rsvp = rsvpRepository.findById(new RsvpId(playerId, gameId))
            .orElseThrow(() -> new NoSuchElementException("RSVP does not exist!"));
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new NoSuchElementException("Game does not exist!"));
        if (game.getStatus() == GameStatus.CANCELLED) {
            throw new IllegalStateException("Cannot RSVP to a cancelled game");
        }

        rsvp.changeStatus(status);
    }

    public void deleteRsvp(Long playerId, Long gameId) {
        Rsvp rsvp = rsvpRepository.findById(new RsvpId(playerId, gameId))
            .orElseThrow(() -> new NoSuchElementException("RSVP does not exist!"));
        rsvpRepository.delete(rsvp);
    }

    public List<PlayerRsvpSummary> getRsvpsForGameId(Long gameId) {
        if (!gameRepository.existsById(gameId)) {
            throw new IllegalStateException("Game does not exist!");
        }
        List<PlayerRsvpSummary> rsvps = rsvpRepository.findSummariesByGameId(gameId);
        return rsvps;
    }

    public List<GameRsvpSummary> getRsvpsForPlayerId(Long playerId) {
        if (!playerRepository.existsById(playerId)) {
            throw new IllegalStateException("Player does not exist!");
        }
        List<GameRsvpSummary> rsvps = rsvpRepository.findSummariesByPlayerId(playerId);
        return rsvps;
    }
}
package com.aidenjee.jumpbeyondfriends;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service 
@Transactional
public class GameService {
    private final GameRepository gameRepository;
    private final RsvpRepository rsvpRepository;

    public GameService(GameRepository gameRepository, RsvpRepository rsvpRepository) {
        this.gameRepository = gameRepository;
        this.rsvpRepository = rsvpRepository;
    }

    public GameSummary getGameSummary(Long gameId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new NoSuchElementException("Game does not exist!"));
        long goingCount = rsvpRepository.countByGame_IdAndStatus(gameId, RsvpStatus.GOING);
        return new GameSummary(game, goingCount);
    }

    public long countRsvpsByStatus(Long gameId, RsvpStatus status) {
        if (!gameRepository.existsById(gameId)) {
            throw new NoSuchElementException("Game does not exist!");
        }
        return rsvpRepository.countByGame_IdAndStatus(gameId, status);
    }

    public boolean isReadyToBook(Long gameId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new NoSuchElementException("Game does not exist!"));
        if (game.getStatus() != GameStatus.PROPOSED) {
            throw new IllegalStateException("Game state must be proposed");
        }
        return rsvpRepository.countByGame_IdAndStatus(gameId, RsvpStatus.GOING) >= game.getThreshold();
    }

    public void bookGame(Long gameId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new NoSuchElementException("Game does not exist!"));
        if (game.getStatus() != GameStatus.PROPOSED) {
            throw new IllegalStateException("Game state must be proposed");
        }
        game.setStatus(GameStatus.BOOKED);
    }

    public void cancelGame(Long gameId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new NoSuchElementException("Game does not exist!"));
        game.setStatus(GameStatus.CANCELLED);
    }

}

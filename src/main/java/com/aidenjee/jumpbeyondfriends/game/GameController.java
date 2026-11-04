package com.aidenjee.jumpbeyondfriends.game;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;




@RestController 
public class GameController {
    private final GameService gameService;

    public record GameDetailsResponse(
        Long gameId,
        Instant startTime,
        Instant endTime,
        GameStatus status,
        int threshold,
        long goingCount,
        boolean meetsAttendanceThreshold
    ) {}

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/api/games/{gameId}")
    @ResponseStatus(HttpStatus.OK)
    public GameDetailsResponse getGameDetails(@PathVariable Long gameId) {
        GameSummary summary = gameService.getGameSummary(gameId);
        Game game = summary.game();
        long goingCount = summary.goingCount();
        return new GameDetailsResponse(
            gameId, 
            game.getStartTime(), 
            game.getEndTime(), 
            game.getStatus(), 
            game.getThreshold(), 
            goingCount, 
            goingCount >= game.getThreshold()
        );
    }

    @PostMapping("/api/games/{gameId}/book")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bookGame(@PathVariable Long gameId) {
        gameService.bookGame(gameId);
    }

    @PostMapping("/api/games/{gameId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelGame(@PathVariable Long gameId) {
        gameService.cancelGame(gameId);
    }
    
    
    
}

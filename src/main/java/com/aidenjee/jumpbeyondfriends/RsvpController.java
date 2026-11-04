package com.aidenjee.jumpbeyondfriends;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aidenjee.jumpbeyondfriends.RsvpController.GameRsvpResponse;
import com.aidenjee.jumpbeyondfriends.RsvpController.PlayerRsvpResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;




@RestController 
public class RsvpController {
    private final RsvpService rsvpService;

    public record CreateRsvpRequest(@NotNull Long playerId, @NotNull RsvpStatus status) {}

    public record PatchRsvpRequest(@NotNull RsvpStatus status) {}

    public record PlayerRsvpResponse(@NotNull Long playerId, @NotNull RsvpStatus status) {}

    public record GameRsvpResponse(@NotNull Long gameId, @NotNull RsvpStatus status) {}
    
    public RsvpController(RsvpService rsvpService) {
        this.rsvpService = rsvpService;
    }

    @PostMapping("/api/games/{gameId}/rsvps")
    @ResponseStatus(HttpStatus.CREATED)
    public void createRsvp(
        @PathVariable Long gameId, 
        @Valid @RequestBody CreateRsvpRequest req
    ) {
        rsvpService.createRsvp(req.playerId(), gameId, req.status());
    }

    @GetMapping("/api/games/{gameId}/rsvps")
    @ResponseStatus(HttpStatus.OK)
    public List<PlayerRsvpResponse> getRsvpsForGame(@PathVariable Long gameId) {
        return rsvpService.getRsvpsForGameId(gameId)
            .stream()
            .map(summary -> new PlayerRsvpResponse(summary.playerId(), summary.status()))
            .toList();
    }

    @GetMapping("/api/players/{playerId}/rsvps")
    @ResponseStatus(HttpStatus.OK)
    public List<GameRsvpResponse> getRsvpsForPlayer(@PathVariable Long playerId) {
        return rsvpService.getRsvpsForPlayerId(playerId)
            .stream()
            .map(summary -> new GameRsvpResponse(summary.gameId(), summary.status()))
            .toList();
    }

    @PatchMapping("/api/games/{gameId}/rsvps/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void patchRsvp(@PathVariable Long gameId, @PathVariable Long playerId, @Valid @RequestBody PatchRsvpRequest req) {
        rsvpService.updateRsvpStatus(playerId, gameId, req.status());
    } 
}

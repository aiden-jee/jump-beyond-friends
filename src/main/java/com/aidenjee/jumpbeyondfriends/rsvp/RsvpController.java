package com.aidenjee.jumpbeyondfriends.rsvp;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;




@RestController 
public class RsvpController {
    private final RsvpService rsvpService;

    public record CreateRsvpRequest(@NotNull RsvpStatus status) {}

    public record PatchRsvpRequest(@NotNull RsvpStatus status) {}

    public record PlayerRsvpResponse(@NotNull Long playerId, @NotNull RsvpStatus status) {}

    public record GameRsvpResponse(@NotNull Long gameId, @NotNull RsvpStatus status) {}
    
    public RsvpController(RsvpService rsvpService) {
        this.rsvpService = rsvpService;
    }

    @PostMapping("/api/games/{gameId}/rsvps")
    @ResponseStatus(HttpStatus.CREATED)
    public void createRsvp(
        @AuthenticationPrincipal OidcUser user,
        @PathVariable Long gameId, 
        @Valid @RequestBody CreateRsvpRequest req
    ) {
        rsvpService.createRsvp(user.getSubject(), gameId, req.status());
    }

    @GetMapping("/api/games/{gameId}/rsvps")
    @ResponseStatus(HttpStatus.OK)
    public List<RsvpPlayerResponse> getRsvpsForGame(@PathVariable Long gameId) {
        return rsvpService.getRsvpsForGameId(gameId);
    }

    @GetMapping("/api/players/{playerId}/rsvps")
    @ResponseStatus(HttpStatus.OK)
    public List<GameRsvpResponse> getRsvpsForPlayer(@PathVariable Long playerId) {
        return rsvpService.getRsvpsForPlayerId(playerId)
            .stream()
            .map(summary -> new GameRsvpResponse(summary.gameId(), summary.status()))
            .toList();
    }

    @PatchMapping("/api/games/{gameId}/rsvps")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void patchRsvp(@AuthenticationPrincipal OidcUser user, @PathVariable Long gameId, @Valid @RequestBody PatchRsvpRequest req) {
        rsvpService.updateRsvpStatus(user.getSubject(), gameId, req.status());
    } 
}

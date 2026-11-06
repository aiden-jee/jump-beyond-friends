package com.aidenjee.jumpbeyondfriends.player;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;



@RestController 
public class PlayerController { 
    private final PlayerService playerService;

    public record PlayerRequest(
           @NotBlank @Size(min = 3, max = 50) String name
    ) {}

    public record PlayerResponse(
        @NotNull Long playerId,
        @NotNull String name
    ) {}

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/api/players")
    public PlayerResponse createPlayer(@Valid @RequestBody PlayerRequest req) {
        Player player = playerService.createPlayer(req.name());
        return new PlayerResponse(player.getId(), player.getName());
    }

    @GetMapping("/api/players")
    public PlayerResponse getPlayerByName(@RequestParam String name) {
        Player player = playerService.getPlayer(name);
        return new PlayerResponse(player.getId(), player.getName());
    }
    
    

}

package com.aidenjee.jumpbeyondfriends.auth;


import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.aidenjee.jumpbeyondfriends.player.Player;
import com.aidenjee.jumpbeyondfriends.player.PlayerService;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
public class AuthController {

    private final PlayerService playerService;

    public record RegisterRequest(
        String code
    ) {}

    public record UserResponse(
        Long id,
        String name,
        String avatarUrl
    ) {}

    public AuthController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/api/me")
    public UserResponse registerUser(@AuthenticationPrincipal OidcUser user, @RequestBody RegisterRequest req) {
        Player player = playerService.createPlayer(user.getFullName(), user.getSubject(), user.getPicture(), req.code());
        return new UserResponse(
            player.getId(), 
            player.getName(), 
            player.getAvatarUrl()
        );
    }

    @GetMapping("/api/me")
    public UserResponse getUserInfo(@AuthenticationPrincipal OidcUser user) {
        Player player = playerService.getPlayer(user.getSubject());
        return new UserResponse(
            player.getId(), 
            player.getName(), 
            player.getAvatarUrl()
        );
    }
    
}
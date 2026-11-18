package com.aidenjee.jumpbeyondfriends.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.aidenjee.jumpbeyondfriends.player.PlayerRepository;

@Service 
public class AuthService {
    private final PlayerRepository playerRepository;

    public AuthService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public boolean isRegistered(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof OidcUser user)) {
            return false;
        }
        return playerRepository.existsByGoogleSub(user.getSubject());
    }
}

package com.aidenjee.jumpbeyondfriends.player;

import java.time.Instant;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aidenjee.jumpbeyondfriends.auth.EnrollService;
import com.aidenjee.jumpbeyondfriends.exception.BusinessConflictException;
import com.aidenjee.jumpbeyondfriends.exception.InvalidCodeException;

import jakarta.transaction.Transactional;

@Service
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final EnrollService enrollService;

    public PlayerService(PlayerRepository playerRepository, EnrollService enrollService) {
        this.playerRepository = playerRepository;
        this.enrollService = enrollService;
    }

    public Player createPlayer(String name) {
        if (playerRepository.existsByName(name)) {
            throw new BusinessConflictException("Player name already exists");
        }
        Player player = new Player(name);
        playerRepository.save(player);
        return player;
    }

    @Transactional
    public Player createPlayer(String name, String googleSub, String avatarUrl, String code) {
        if (playerRepository.existsByGoogleSub(googleSub)) {
            throw new BusinessConflictException("Player already exists!");
        }

        if (!enrollService.isValidCode(code)) {
            throw new InvalidCodeException("Incorrect code");
        }

        Player player = new Player(name, googleSub, avatarUrl);
        return playerRepository.save(player);
    }

    public Player getPlayer(String googleSub) {
        Player player = playerRepository.findByGoogleSub(googleSub)
            .orElseThrow(() -> new NoSuchElementException("Player does not exist"));
        return player;
    }
}

package com.aidenjee.jumpbeyondfriends.player;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.aidenjee.jumpbeyondfriends.exception.BusinessConflictException;

@Service
public class PlayerService {
    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public Player createPlayer(String name) {
        if (playerRepository.existsByName(name)) {
            throw new BusinessConflictException("Player name already exists");
        }
        Player player = new Player(name);
        playerRepository.save(player);
        return player;
    }

    public Player getPlayer(String name) {
        Player player = playerRepository.findByName(name)
            .orElseThrow(() -> new NoSuchElementException("Player does not exist"));
        return player;
    }
}

package com.aidenjee.jumpbeyondfriends.player;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long>{
    boolean existsByName(String name);

    Optional<Player> getByName(String name);
}

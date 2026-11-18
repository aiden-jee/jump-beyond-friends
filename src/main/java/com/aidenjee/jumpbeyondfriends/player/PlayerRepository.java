package com.aidenjee.jumpbeyondfriends.player;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface PlayerRepository extends JpaRepository<Player, Long>{
    boolean existsByName(String name);

    boolean existsByGoogleSub(String googleSub);

    Optional<Player> findByGoogleSub(String googleSub);

    Optional<Player> findByName(String name);
}

package com.aidenjee.jumpbeyondfriends;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
    
}

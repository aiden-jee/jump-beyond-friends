package com.aidenjee.jumpbeyondfriends;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RsvpRepository extends JpaRepository<Rsvp, RsvpId>{
        List<Rsvp> findByGameId(Long gameId);
        List<Rsvp> findByPlayerId(Long playerId);
}

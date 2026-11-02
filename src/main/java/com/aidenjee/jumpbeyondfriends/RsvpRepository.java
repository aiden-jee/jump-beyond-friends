package com.aidenjee.jumpbeyondfriends;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RsvpRepository extends JpaRepository<Rsvp, RsvpId>{
        List<Rsvp> findByGame_Id(Long gameId);
        List<Rsvp> findByPlayer_Id(Long playerId);
}

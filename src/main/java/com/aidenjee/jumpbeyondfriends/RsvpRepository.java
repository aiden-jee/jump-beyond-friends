package com.aidenjee.jumpbeyondfriends;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RsvpRepository extends JpaRepository<Rsvp, RsvpId>{
        List<Rsvp> findByGame_Id(Long gameId);
        List<Rsvp> findByPlayer_Id(Long playerId);

        long countByGame_IdAndStatus(Long gameId, RsvpStatus status);

        @Query("""
                SELECT new com.aidenjee.jumpbeyondfriends.PlayerRsvpSummary(
                        r.id.playerId,
                        r.status
                )
                FROM Rsvp r
                WHERE r.id.gameId = :gameId
                """)
        List<PlayerRsvpSummary> findSummariesByGameId(@Param("gameId") Long gameId);

        @Query("""
                SELECT new com.aidenjee.jumpbeyondfriends.GameRsvpSummary(
                        r.id.gameId,
                        r.status
                )
                FROM Rsvp r
                WHERE r.id.playerId = :playerId
                """)
        List<GameRsvpSummary> findSummariesByPlayerId(@Param("playerId") Long playerId);


}

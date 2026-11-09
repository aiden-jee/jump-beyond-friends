package com.aidenjee.jumpbeyondfriends.game;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aidenjee.jumpbeyondfriends.rsvp.RsvpStatus;

import jakarta.persistence.LockModeType;

public interface GameRepository extends JpaRepository<Game, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Game g WHERE g.id = :gameId")
    Optional<Game> findByIdForWrite(@Param("gameId") Long gameId);

    List<Game> findByStartTimeAfterAndStatusNotOrderByStartTimeAsc(
        Instant cutoff,
        GameStatus excludedStatus
    );

    @Query("""
        SELECT new com.aidenjee.jumpbeyondfriends.game.GameSummary(
            g,
            COUNT(r)
        )
        FROM Game g
        LEFT JOIN Rsvp r
            ON r.game = g AND r.status = :status
        WHERE g.startTime > :cutoff
        AND g.status <> :excludedStatus
        GROUP BY g
        ORDER BY g.startTime ASC
        """)
    List<GameSummary> findUpcomingGameSummaries(
        @Param("cutoff") Instant cutoff,
        @Param("status") RsvpStatus status,
        @Param("excludedStatus") GameStatus excludedStatus
    );
}

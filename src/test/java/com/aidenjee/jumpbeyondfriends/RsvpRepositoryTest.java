package com.aidenjee.jumpbeyondfriends;

import com.aidenjee.jumpbeyondfriends.game.Game;
import com.aidenjee.jumpbeyondfriends.game.GameRepository;
import com.aidenjee.jumpbeyondfriends.player.Player;
import com.aidenjee.jumpbeyondfriends.player.PlayerRepository;
import com.aidenjee.jumpbeyondfriends.rsvp.Rsvp;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpRepository;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;


@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
public class RsvpRepositoryTest {
    @Autowired 
    private RsvpRepository rsvpRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired 
    private GameRepository gameRepository;

    @Autowired 
    private PlayerRepository playerRepository;

    @Test
    public void createAndSaveRsvpShouldPersist() {
        Instant start = Instant.parse("2100-01-01T18:00:00Z");
        Instant end = start.plus(2, ChronoUnit.HOURS);

        Player player = new Player("RSVP Test Player");
        Game game = new Game(start, end);
        playerRepository.save(player);
        gameRepository.save(game);

        Rsvp rsvp = new Rsvp(player, game, RsvpStatus.GOING);
        rsvpRepository.save(rsvp);

        entityManager.flush();
        entityManager.clear();

        Rsvp repoRsvp = rsvpRepository.findById(rsvp.getId())
            .orElseThrow(() -> new NoSuchElementException());
        assertEquals(rsvp.getId(), repoRsvp.getId());
        assertEquals(rsvp.getStatus(), repoRsvp.getStatus());
    }
}

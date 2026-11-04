package com.aidenjee.jumpbeyondfriends;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.aidenjee.jumpbeyondfriends.game.Game;
import com.aidenjee.jumpbeyondfriends.game.GameRepository;
import com.aidenjee.jumpbeyondfriends.player.Player;
import com.aidenjee.jumpbeyondfriends.player.PlayerRepository;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpRepository;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpService;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpStatus;

@SpringBootTest
@ActiveProfiles("test")
public class RsvpConcurrencyTest {

    @Autowired
    private RsvpService rsvpService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private RsvpRepository rsvpRepository;

    private Long gameId;
    private Long playerId;

    @BeforeEach
    void setUp() {
        Game game = createTestGame();
        Player player = createTestPlayer();

        gameId = game.getId();
        playerId = player.getId();
    }

    @AfterEach
    void cleanUp() {
        rsvpRepository.deleteAll();
        gameRepository.deleteAll();
        playerRepository.deleteAll();
    }

    private Game createTestGame() {
        Instant before = Instant.now().plus(7, ChronoUnit.DAYS);
        Instant after = before.plus(2, ChronoUnit.HOURS);
        Game game = new Game(before, after);
        return gameRepository.saveAndFlush(game);
    }

    private Player createTestPlayer() {
        Player player = new Player("Aiden Jee");
        return playerRepository.saveAndFlush(player);
    }

@Test
void concurrentIdenticalRsvpWritesShouldOnlyCreateOne() throws Exception {
    int threadCount = 3;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch ready = new CountDownLatch(threadCount);
    CountDownLatch start = new CountDownLatch(1);

    List<Future<Void>> futures = new ArrayList<>();

    try {
        for (int i = 0; i < threadCount; i++) {
            Future<Void> future = executor.submit(() -> {
                ready.countDown();
                start.await();
                rsvpService.createRsvp(
                    playerId,
                    gameId,
                    RsvpStatus.GOING
                );
                return null;
            });
            futures.add(future);
        }

        assertTrue(
            ready.await(10, TimeUnit.SECONDS),
            "Not all workers became ready"
        );
        start.countDown();

        int successCnt = 0;
        int duplicateCnt = 0;

        for (Future<Void> future : futures) {
            try {
                future.get(10, TimeUnit.SECONDS);
                successCnt++;

            } catch (ExecutionException e) {
                Throwable cause = e.getCause();

                if (cause instanceof IllegalStateException
                        && "RSVP already exists".equals(cause.getMessage())) {
                    duplicateCnt++;
                } else {
                    throw e;
                }
            }
        }

        assertEquals(1, successCnt);
        assertEquals(2, duplicateCnt);
        assertEquals(1, rsvpRepository.findByGame_Id(gameId).size());

    } finally {
        start.countDown();
        executor.shutdownNow();
        assertTrue(
            executor.awaitTermination(10, TimeUnit.SECONDS),
            "Executor did not terminate"
        );
    }
}
}
package com.aidenjee.jumpbeyondfriends;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.aidenjee.jumpbeyondfriends.game.Game;
import com.aidenjee.jumpbeyondfriends.game.GameRepository;
import com.aidenjee.jumpbeyondfriends.player.Player;
import com.aidenjee.jumpbeyondfriends.player.PlayerRepository;
import com.aidenjee.jumpbeyondfriends.rsvp.Rsvp;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpId;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpRepository;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpStatus;

import static com.aidenjee.jumpbeyondfriends.TestGoogleUsers.googleUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {
    private static final String PLAYER_B_SUB = "google-player-b";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private RsvpRepository rsvpRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void unauthenticatedUserRequestsGamesShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/games"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void googleAuthenticatedUserWithoutPlayerRequestsGamesShouldReturn403() throws Exception {
        createPlayer("Player B", PLAYER_B_SUB);

        mockMvc.perform(get("/api/games").with(googleUser(TestGoogleUsers.GOOGLE_SUB)))
            .andExpect(status().isForbidden());
    }

    @Test
    void enrolledPlayerRequestsGamesShouldReturn200() throws Exception {
        createPlayer(TestGoogleUsers.NAME, TestGoogleUsers.GOOGLE_SUB);
        Game game = createGame();

        mockMvc.perform(get("/api/games").with(googleUser(TestGoogleUsers.GOOGLE_SUB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.gameId == %s)]", game.getId()).exists());
    }

    @Test
    void playerWhoseRowWasRemovedRequestsGamesShouldReturn403() throws Exception {
        Player player = createPlayer(TestGoogleUsers.NAME, TestGoogleUsers.GOOGLE_SUB);
        var login = googleUser(TestGoogleUsers.GOOGLE_SUB);
        mockMvc.perform(get("/api/games").with(login))
            .andExpect(status().isOk());

        // The same Google identity loses access when its Player row is removed.
        playerRepository.delete(player);
        flushAndClear();
        assertFalse(playerRepository.existsByGoogleSub(TestGoogleUsers.GOOGLE_SUB));

        mockMvc.perform(get("/api/games").with(login))
            .andExpect(status().isForbidden());
    }

    @Test
    void playerACreatesRsvpShouldPersistRsvpBelongingToA() throws Exception {
        Player playerA = createPlayer(TestGoogleUsers.NAME, TestGoogleUsers.GOOGLE_SUB);
        Game game = createGame();

        mockMvc.perform(post("/api/games/{gameId}/rsvps", game.getId())
                .with(googleUser(TestGoogleUsers.GOOGLE_SUB)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "status": "GOING" }
                    """))
            .andExpect(status().isCreated());
        flushAndClear();

        Rsvp rsvp = rsvpRepository.findById(new RsvpId(playerA.getId(), game.getId())).orElseThrow();
        assertEquals(playerA.getId(), rsvp.getId().getPlayerId());
        assertEquals(RsvpStatus.GOING, rsvp.getStatus());
        assertEquals(1, rsvpRepository.findByGame_Id(game.getId()).size());
    }

    @Test
    void playerACannotCreateRsvpForPlayerBBySupplyingPlayerId() throws Exception {
        Player playerA = createPlayer(TestGoogleUsers.NAME, TestGoogleUsers.GOOGLE_SUB);
        Player playerB = createPlayer("Player B", PLAYER_B_SUB);
        Game game = createGame();
        rsvpRepository.saveAndFlush(new Rsvp(playerB, game, RsvpStatus.MAYBE));

        mockMvc.perform(post("/api/games/{gameId}/rsvps", game.getId())
                .with(googleUser(TestGoogleUsers.GOOGLE_SUB)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "playerId": %d, "status": "GOING" }
                    """.formatted(playerB.getId())))
            .andExpect(status().isCreated());
        flushAndClear();

        assertEquals(RsvpStatus.GOING, storedStatus(playerA, game));
        assertEquals(RsvpStatus.MAYBE, storedStatus(playerB, game));
    }

    @Test
    void playerACannotUpdatePlayerBRsvpBySupplyingPlayerId() throws Exception {
        Player playerA = createPlayer(TestGoogleUsers.NAME, TestGoogleUsers.GOOGLE_SUB);
        Player playerB = createPlayer("Player B", PLAYER_B_SUB);
        Game game = createGame();
        rsvpRepository.save(new Rsvp(playerA, game, RsvpStatus.MAYBE));
        rsvpRepository.saveAndFlush(new Rsvp(playerB, game, RsvpStatus.GOING));

        mockMvc.perform(patch("/api/games/{gameId}/rsvps", game.getId())
                .with(googleUser(TestGoogleUsers.GOOGLE_SUB)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "playerId": %d, "status": "NOT_GOING" }
                    """.formatted(playerB.getId())))
            .andExpect(status().isNoContent());
        flushAndClear();

        assertEquals(RsvpStatus.NOT_GOING, storedStatus(playerA, game));
        assertEquals(RsvpStatus.GOING, storedStatus(playerB, game));
    }

    @Test
    void playerAWithoutOwnRsvpCannotUpdatePlayerBRsvp() throws Exception {
        Player playerA = createPlayer(TestGoogleUsers.NAME, TestGoogleUsers.GOOGLE_SUB);
        Player playerB = createPlayer("Player B", PLAYER_B_SUB);
        Game game = createGame();
        rsvpRepository.saveAndFlush(new Rsvp(playerB, game, RsvpStatus.GOING));

        mockMvc.perform(patch("/api/games/{gameId}/rsvps", game.getId())
                .with(googleUser(TestGoogleUsers.GOOGLE_SUB)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "playerId": %d, "status": "NOT_GOING" }
                    """.formatted(playerB.getId())))
            .andExpect(status().isNotFound());
        entityManager.clear();

        assertEquals(RsvpStatus.GOING, storedStatus(playerB, game));
        assertFalse(rsvpRepository.existsById(new RsvpId(playerA.getId(), game.getId())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PATCH", "DELETE"})
    void playerACannotTargetPlayerBThroughOldRsvpUrl(String method) throws Exception {
        createPlayer(TestGoogleUsers.NAME, TestGoogleUsers.GOOGLE_SUB);
        Player playerB = createPlayer("Player B", PLAYER_B_SUB);
        Game game = createGame();
        rsvpRepository.saveAndFlush(new Rsvp(playerB, game, RsvpStatus.GOING));

        mockMvc.perform(request(HttpMethod.valueOf(method), "/api/games/{gameId}/rsvps/{playerId}",
                    game.getId(), playerB.getId())
                .with(googleUser(TestGoogleUsers.GOOGLE_SUB)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "status": "NOT_GOING" }
                    """))
            .andExpect(status().isNotFound());
        flushAndClear();

        assertEquals(RsvpStatus.GOING, storedStatus(playerB, game));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"incorrect-code", " "})
    void incorrectEnrollmentCodeShouldNotCreatePlayer(String code) throws Exception {
        long playerCount = playerRepository.count();
        String body = code == null ? "{ \"code\": null }" : "{ \"code\": \"" + code + "\" }";

        mockMvc.perform(post("/api/me")
                .with(googleUser(TestGoogleUsers.GOOGLE_SUB)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Incorrect code"));
        entityManager.clear();

        assertEquals(playerCount, playerRepository.count());
        assertFalse(playerRepository.existsByGoogleSub(TestGoogleUsers.GOOGLE_SUB));
    }

    @Test
    void correctEnrollmentCodeShouldCreatePlayerAndGrantAccess() throws Exception {
        long playerCount = playerRepository.count();

        mockMvc.perform(post("/api/me")
                .with(googleUser(TestGoogleUsers.GOOGLE_SUB)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"code\": \"" + TestGoogleUsers.ENROLLMENT_CODE + "\" }"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name").value(TestGoogleUsers.NAME))
            .andExpect(jsonPath("$.avatarUrl").value(TestGoogleUsers.AVATAR_URL));
        flushAndClear();

        Player player = playerRepository.findByGoogleSub(TestGoogleUsers.GOOGLE_SUB).orElseThrow();
        assertEquals(playerCount + 1, playerRepository.count());
        assertEquals(TestGoogleUsers.GOOGLE_SUB, player.getGoogleSub());
        assertEquals(TestGoogleUsers.NAME, player.getName());
        assertEquals(TestGoogleUsers.AVATAR_URL, player.getAvatarUrl());

        mockMvc.perform(get("/api/games").with(googleUser(TestGoogleUsers.GOOGLE_SUB)))
            .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedUserCannotEnrollEvenWithCorrectCode() throws Exception {
        long playerCount = playerRepository.count();

        mockMvc.perform(post("/api/me").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"code\": \"" + TestGoogleUsers.ENROLLMENT_CODE + "\" }"))
            .andExpect(status().isUnauthorized());

        assertEquals(playerCount, playerRepository.count());
    }

    @Test
    void enrollmentWithoutCsrfTokenShouldNotCreatePlayer() throws Exception {
        long playerCount = playerRepository.count();

        mockMvc.perform(post("/api/me").with(googleUser(TestGoogleUsers.GOOGLE_SUB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"code\": \"" + TestGoogleUsers.ENROLLMENT_CODE + "\" }"))
            .andExpect(status().isForbidden());

        assertEquals(playerCount, playerRepository.count());
        assertFalse(playerRepository.existsByGoogleSub(TestGoogleUsers.GOOGLE_SUB));
    }

    private Player createPlayer(String name, String subject) {
        return playerRepository.saveAndFlush(new Player(name, subject, null));
    }

    private Game createGame() {
        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        return gameRepository.saveAndFlush(new Game(start, start.plus(2, ChronoUnit.HOURS)));
    }

    private RsvpStatus storedStatus(Player player, Game game) {
        return rsvpRepository.findById(new RsvpId(player.getId(), game.getId())).orElseThrow().getStatus();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}

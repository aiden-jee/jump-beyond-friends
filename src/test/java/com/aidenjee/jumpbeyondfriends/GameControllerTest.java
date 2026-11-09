package com.aidenjee.jumpbeyondfriends;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aidenjee.jumpbeyondfriends.game.Game;
import com.aidenjee.jumpbeyondfriends.game.GameController;
import com.aidenjee.jumpbeyondfriends.game.GameService;

@WebMvcTest(GameController.class)
public class GameControllerTest {
    @Autowired 
    MockMvc mockMvc;

    @MockitoBean 
    GameService gameService;

    private Game createGame(Instant start) {
        Game game = new Game(start, start.plus(2, ChronoUnit.HOURS), 4);
        return game;
    }

    @Test 
    public void createGameShouldReturn201() throws Exception {
        Instant start = Instant.parse("2040-01-01T00:00:00Z");
        Instant end = start.plus(2, ChronoUnit.HOURS);
        Game game = createGame(start);
        when(gameService.createGame(start, end, 4))
            .thenReturn(game);
        mockMvc.perform(post("/api/games")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "startTime": "2040-01-01T00:00:00Z",
                    "endTime": "2040-01-01T02:00:00Z",
                    "threshold": 4
                }
            """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.startTime").value("2040-01-01T00:00:00Z"))
            .andExpect(jsonPath("$.endTime").value("2040-01-01T02:00:00Z"))
            .andExpect(jsonPath("$.threshold").value(4));

        verify(gameService).createGame(start, end, 4);
    }
}

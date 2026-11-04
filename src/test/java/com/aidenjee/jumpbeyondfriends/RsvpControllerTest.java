package com.aidenjee.jumpbeyondfriends;

import com.aidenjee.jumpbeyondfriends.rsvp.GameRsvpSummary;
import com.aidenjee.jumpbeyondfriends.rsvp.PlayerRsvpSummary;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpController;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpService;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpStatus;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(RsvpController.class)
public class RsvpControllerTest {
    @Autowired 
    private MockMvc mockMvc;

    @MockitoBean
    private RsvpService rsvpService;

    @Test 
    public void createRsvpShouldReturn201() throws Exception {
        mockMvc.perform(post("/api/games/" + 2L + "/rsvps")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "playerId": 2, "status": "GOING" }
            """))
            .andExpect(status().isCreated());
    }

    @Test 
    public void getRsvpsForGameShouldReturn200AndJson() throws Exception {
        PlayerRsvpSummary example = new PlayerRsvpSummary(1L, RsvpStatus.GOING);
        PlayerRsvpSummary example2 = new PlayerRsvpSummary(2L, RsvpStatus.MAYBE);

        List<PlayerRsvpSummary> rsvps = List.of(example, example2);
        when(rsvpService.getRsvpsForGameId(3L)).thenReturn(rsvps);
        mockMvc.perform(get("/api/games/" + 3L + "/rsvps"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].playerId").value(1))
            .andExpect(jsonPath("$[0].status").value("GOING"))
            .andExpect(jsonPath("$[1].playerId").value(2))
            .andExpect(jsonPath("$[1].status").value("MAYBE"));
        
        verify(rsvpService).getRsvpsForGameId(3L);
    }

    @Test 
    public void getRsvpsForPlayerShouldReturn200AndJson() throws Exception {
        GameRsvpSummary example = new GameRsvpSummary(1L, RsvpStatus.GOING);
        GameRsvpSummary example2 = new GameRsvpSummary(2L, RsvpStatus.MAYBE);

        List<GameRsvpSummary> rsvps = List.of(example, example2);
        when(rsvpService.getRsvpsForPlayerId(3L)).thenReturn(rsvps);
        mockMvc.perform(get("/api/players/" + 3L + "/rsvps"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].gameId").value(1))
            .andExpect(jsonPath("$[0].status").value("GOING"))
            .andExpect(jsonPath("$[1].gameId").value(2))
            .andExpect(jsonPath("$[1].status").value("MAYBE"));
        
        verify(rsvpService).getRsvpsForPlayerId(3L);
    }

    @Test 
    public void patchRsvpShouldReturn204() throws Exception {
        mockMvc.perform(patch("/api/games/" + 2L + "/rsvps/" + 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "status": "GOING" }
            """))
            .andExpect(status().isNoContent());
    }
}

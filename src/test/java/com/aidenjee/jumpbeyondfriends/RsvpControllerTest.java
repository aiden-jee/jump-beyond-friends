package com.aidenjee.jumpbeyondfriends;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static com.aidenjee.jumpbeyondfriends.TestGoogleUsers.googleUser;
import com.aidenjee.jumpbeyondfriends.exception.BusinessConflictException;
import com.aidenjee.jumpbeyondfriends.player.PlayerRepository;
import com.aidenjee.jumpbeyondfriends.rsvp.GameRsvpSummary;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpController;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpPlayerResponse;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpService;
import com.aidenjee.jumpbeyondfriends.rsvp.RsvpStatus;


@WebMvcTest(RsvpController.class)
@Import(ControllerSecurityTestConfig.class)
public class RsvpControllerTest {
    @Autowired 
    private MockMvc mockMvc;

    @MockitoBean
    private RsvpService rsvpService;

    @MockitoBean
    private PlayerRepository playerRepository;

    @BeforeEach
    void enrollCurrentUser() {
        when(playerRepository.existsByGoogleSub(TestGoogleUsers.GOOGLE_SUB)).thenReturn(true);
    }

    @Test 
    public void createRsvpShouldReturn201() throws Exception {
        mockMvc.perform(post("/api/games/" + 2L + "/rsvps")
            .with(googleUser(TestGoogleUsers.GOOGLE_SUB)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "status": "GOING" }
            """))
            .andExpect(status().isCreated());
        verify(rsvpService).createRsvp(TestGoogleUsers.GOOGLE_SUB, 2L, RsvpStatus.GOING);
    }

    @Test 
    public void createRsvpWithBadStatusShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/games/" + 2L + "/rsvps")
            .with(googleUser(TestGoogleUsers.GOOGLE_SUB)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "status": "ATTENDING" }
            """))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(rsvpService);
    }

    @Test
    public void createDuplicateRsvpShouldReturn409() throws Exception {
        doThrow(new BusinessConflictException("RSVP already exists"))
            .when(rsvpService).createRsvp(TestGoogleUsers.GOOGLE_SUB, 2L, RsvpStatus.GOING);

        mockMvc.perform(post("/api/games/2/rsvps")
            .with(googleUser(TestGoogleUsers.GOOGLE_SUB)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "status": "GOING" }
            """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("RSVP already exists"));
    }

    @Test 
    public void getRsvpsForPlayerShouldReturn200AndJson() throws Exception {
        GameRsvpSummary example = new GameRsvpSummary(1L, RsvpStatus.GOING);
        GameRsvpSummary example2 = new GameRsvpSummary(2L, RsvpStatus.MAYBE);

        List<GameRsvpSummary> rsvps = List.of(example, example2);
        when(rsvpService.getRsvpsForPlayerId(3L)).thenReturn(rsvps);
        mockMvc.perform(get("/api/players/" + 3L + "/rsvps")
            .with(googleUser(TestGoogleUsers.GOOGLE_SUB)))
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
        mockMvc.perform(patch("/api/games/" + 2L + "/rsvps")
            .with(googleUser(TestGoogleUsers.GOOGLE_SUB)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "status": "GOING" }
            """))
            .andExpect(status().isNoContent());
        verify(rsvpService).updateRsvpStatus(TestGoogleUsers.GOOGLE_SUB, 2L, RsvpStatus.GOING);
    }

    @Test
    void getRsvpsForGameShouldReturnPlayerNamesAndStatuses() throws Exception {
        when(rsvpService.getRsvpsForGameId(3L)).thenReturn(List.of(
            new RsvpPlayerResponse(1L, "Player A", "placeholder", RsvpStatus.GOING),
            new RsvpPlayerResponse(2L, "Player B", "placeholder", RsvpStatus.MAYBE)
        ));

        mockMvc.perform(get("/api/games/3/rsvps").with(googleUser(TestGoogleUsers.GOOGLE_SUB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].playerId").value(1))
            .andExpect(jsonPath("$[0].name").value("Player A"))
            .andExpect(jsonPath("$[0].status").value("GOING"))
            .andExpect(jsonPath("$[1].playerId").value(2))
            .andExpect(jsonPath("$[1].name").value("Player B"))
            .andExpect(jsonPath("$[1].status").value("MAYBE"));
    }
}

package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.Team;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.WebsocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.RemovePlayerDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(LobbyController.class)
public class LobbyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LobbyService lobbyService;

    @MockBean
    private WebsocketService websocketService;

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class LobbyCreation {

        @Test
        public void getLobbyByCode_returnsLobby() throws Exception {
            Lobby mockLobby = new Lobby();
            mockLobby.setId(99L);
            mockLobby.setLobbyName("JoinableLobby");
            mockLobby.setGameMode(GameMode.CLASSIC);
            mockLobby.setLobbyCode(1234);

            when(lobbyService.getOrCreateLobby(1234, false)).thenReturn(mockLobby);

            mockMvc.perform(get("/lobby?code=1234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(99))
                    .andExpect(jsonPath("$.lobbyName").value("JoinableLobby"))
                    .andExpect(jsonPath("$.gameMode").value("CLASSIC"))
                    .andExpect(jsonPath("$.lobbyCode").value(1234));
        }

        @Test
        public void getLobbyByCode_withOpenFlag_returnsLobby() throws Exception {
            Lobby mockLobby = new Lobby();
            mockLobby.setId(88L);
            mockLobby.setLobbyName("OpenLobby");
            mockLobby.setGameMode(GameMode.CLASSIC);
            mockLobby.setLobbyCode(5678);
            mockLobby.setOpenForLostPlayers(true);

            when(lobbyService.getOrCreateLobby(5678, true)).thenReturn(mockLobby);

            mockMvc.perform(get("/lobby?code=5678&autoCreate=true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(88))
                    .andExpect(jsonPath("$.lobbyName").value("OpenLobby"))
                    .andExpect(jsonPath("$.gameMode").value("CLASSIC"))
                    .andExpect(jsonPath("$.lobbyCode").value(5678))
                    .andExpect(jsonPath("$.openForLostPlayers").value(true));;

        }

        @Test
        public void getLobbyByCode_notFound_returns404() throws Exception {
            when(lobbyService.getOrCreateLobby(1234, false)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

            mockMvc.perform(get("/lobby?code=1234"))
                    .andExpect(status().isNotFound());
        }
    }
}

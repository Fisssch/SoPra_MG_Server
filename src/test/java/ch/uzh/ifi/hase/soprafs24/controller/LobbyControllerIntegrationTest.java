package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class LobbyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void createLobby_validInput_returnsLobby() throws Exception {
        Map<String, Object> request = Map.of(
                "lobbyName", "Test Lobby",
                "gameMode", GameMode.CLASSIC.toString(),
                "openForLostPlayers", false
        );

        mockMvc.perform(post("/lobby")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lobbyName").value("Test Lobby"))
                .andExpect(jsonPath("$.gameMode").value("CLASSIC"));
    }
    @Test
    public void setTurnDuration_validInput_turnDurationUpdated() throws Exception {
        // 1. Lobby erstellen
        Map<String, Object> request = Map.of(
                "lobbyName", "Timed Lobby",
                "gameMode", GameMode.TIMED.toString(),
                "openForLostPlayers", false
        );

        String response = mockMvc.perform(post("/lobbies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long lobbyId = JsonPath.read(response, "$.id");

        // 2. Turn Duration setzen
        mockMvc.perform(put("/lobbies/" + lobbyId + "/turnDuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("45"))
                .andExpect(status().isNoContent());

        // 3. Überprüfen ob es gesetzt wurde
        mockMvc.perform(get("/lobbies/" + lobbyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnDuration").value(45));
    }
}
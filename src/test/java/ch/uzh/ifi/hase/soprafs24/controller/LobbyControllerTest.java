package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.Team;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.WebsocketService;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;

import org.apache.coyote.Response;

@WebMvcTest(LobbyController.class)
public class LobbyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LobbyService lobbyService;

    @MockBean
    private WebsocketService websocketService;

    private Lobby dummyLobby;

    @BeforeEach
    public void setup() {
        dummyLobby = new Lobby();
        dummyLobby.setId(1L);
        dummyLobby.setCustomWords(Arrays.asList("custom1", "custom2", "custom3"));
    }

    @Nested
    class PlayerJoining {

        @Test
        public void addPlayerToLobby_returnsPlayerWithTeamAndRole() throws Exception {
            Player mockPlayer = new Player();
            mockPlayer.setId(1L);
            mockPlayer.setRole(PlayerRole.SPYMASTER);

            when(lobbyService.addPlayerToLobby(eq(1L), any(Long.class))).thenReturn(mockPlayer);

            mockMvc.perform(put("/lobby/1/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.role").value("SPYMASTER"));
        }
    }

    @Nested
    class RoleHandling {

        @Test
        public void getPlayerRole_returnsRoleDTO() throws Exception {
            Player mockPlayer = new Player();
            mockPlayer.setId(1L);
            mockPlayer.setRole(PlayerRole.SPYMASTER);

            Lobby mockLobby = new Lobby();
            mockLobby.addPlayer(mockPlayer);

            when(lobbyService.getLobbyById(1L)).thenReturn(mockLobby);

            mockMvc.perform(get("/lobby/1/role/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("SPYMASTER"));
        }

        @Test
        public void getPlayerRole_lobbyNotFound_returns404() throws Exception {
            when(lobbyService.getLobbyById(1L)).thenReturn(null);

            mockMvc.perform(get("/lobby/1/role/1"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class TeamHandling {

        @Test
        public void getPlayerTeam_returnsTeamDTO() throws Exception {
            Player mockPlayer = new Player();
            mockPlayer.setId(1L);
            Team team = new Team();
            team.setColor(TeamColor.RED);
            mockPlayer.setTeam(team);

            Lobby mockLobby = new Lobby();
            mockLobby.addPlayer(mockPlayer);

            when(lobbyService.getLobbyById(1L)).thenReturn(mockLobby);

            mockMvc.perform(get("/lobby/1/team/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.color").value("RED"));
        }

        @Test
        public void getPlayerTeam_lobbyNotFound_returns404() throws Exception {
            when(lobbyService.getLobbyById(1L)).thenReturn(null);

            mockMvc.perform(get("/lobby/1/team/1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void getPlayerTeam_teamNotAssigned_returns404() throws Exception {
            Player mockPlayer = new Player();
            mockPlayer.setId(1L);
            mockPlayer.setTeam(null);

            Lobby mockLobby = new Lobby();
            mockLobby.addPlayer(mockPlayer);

            when(lobbyService.getLobbyById(1L)).thenReturn(mockLobby);

            mockMvc.perform(get("/lobby/1/team/1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void changePlayerTeam_setsNewTeam() throws Exception {
            var player = new Player(1L);
            when(lobbyService.changePlayerTeam(1L, 1L, "blue")).thenReturn(player);

            String json = "{ \"color\": \"blue\" }";

            mockMvc.perform(put("/lobby/1/team/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    class ReadyStatusHandling {

        @Test
        public void getPlayerReadyStatus_returnsTrue() throws Exception {
            when(lobbyService.getPlayerReadyStatus(1L, 1L)).thenReturn(true);

            mockMvc.perform(get("/lobby/1/status/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ready").value(true));
        }

        @Test
        public void getPlayerReadyStatus_playerNotFound_returns404() throws Exception {
            when(lobbyService.getPlayerReadyStatus(1L, 1L)).thenReturn(null);

            mockMvc.perform(get("/lobby/1/status/1"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class LobbyCreation {

        @Test
        public void createLobby_returnsLobbyResponseDTO() throws Exception {
            Lobby mockLobby = new Lobby();
            mockLobby.setId(42L);
            mockLobby.setGameMode(GameMode.CLASSIC);
            mockLobby.setLobbyName("TestLobby");
            mockLobby.setLobbyCode(1234);

            when(lobbyService.createLobby("TestLobby", GameMode.CLASSIC)).thenReturn(mockLobby);

            String json = "{ \"lobbyName\": \"TestLobby\", \"gameMode\": \"classic\" }";

            mockMvc.perform(post("/lobby")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(42))
                    .andExpect(jsonPath("$.gameMode").value("CLASSIC"))
                    .andExpect(jsonPath("$.lobbyName").value("TestLobby"));
        }

        @Test
        public void createLobby_invalidGameMode_returns400() throws Exception {
            String invalidJson = "{ \"lobbyName\": \"TestLobby\", \"gameMode\": \"invalidMode\" }";

            mockMvc.perform(post("/lobby")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void getLobbyByCode_returnsLobby() throws Exception {
            Lobby mockLobby = new Lobby();
            mockLobby.setId(99L);
            mockLobby.setLobbyName("JoinableLobby");
            mockLobby.setGameMode(GameMode.CLASSIC);
            mockLobby.setLobbyCode(1234);

            when(lobbyService.getOrCreateLobby(1234)).thenReturn(mockLobby);

            mockMvc.perform(get("/lobby?code=1234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(99))
                    .andExpect(jsonPath("$.lobbyName").value("JoinableLobby"))
                    .andExpect(jsonPath("$.gameMode").value("CLASSIC"))
                    .andExpect(jsonPath("$.lobbyCode").value(1234));
        }

        @Test
        public void getLobbyByCode_notFound_returns404() throws Exception {
            when(lobbyService.getOrCreateLobby(1234)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

            mockMvc.perform(get("/lobby?code=1234"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class CustomWordsHandling {

        @Test
        public void getCustomWords_validRequest_returnsWords() throws Exception {
            Lobby dummyLobby = new Lobby();
            dummyLobby.setCustomWords(Arrays.asList("House", "Dog"));

            when(lobbyService.getLobbyById(1L)).thenReturn(dummyLobby);

            mockMvc.perform(get("/lobby/1/customWords"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]").value("House"))
                    .andExpect(jsonPath("$[1]").value("Dog"))
                    .andExpect(jsonPath("$.length()").value(2)); 
        }

        @Test
        public void getCustomWords_invalidLobby_returnsNotFound() throws Exception {
            when(lobbyService.getLobbyById(1L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

            mockMvc.perform(get("/lobby/1/customWords"))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void countPlayersLobby_validRequest_returnsPlayerStatus() throws Exception {
            dummyLobby.setPlayers(Arrays.asList(createPlayer(true), createPlayer(false), createPlayer(true)));
            when(lobbyService.getLobbyById(1L)).thenReturn(dummyLobby);

            mockMvc.perform(get("/lobby/1/players")
                    .header("Authorization", "Bearer validToken"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPlayers").value(3))
                    .andExpect(jsonPath("$.readyPlayers").value(2));
        }

        @Test
        public void countPlayersLobby_invalidLobby_returnsNotFound() throws Exception {
            when(lobbyService.getLobbyById(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/lobby/99/players")
                    .header("Authorization", "Bearer validToken"))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void addCustomWord_validRequest_returnsNoContent() throws Exception {
            when(lobbyService.addCustomWord(eq(1L), eq("NewWord"))).thenReturn(dummyLobby);
            doNothing().when(websocketService).sendMessage(anyString(), any());

            mockMvc.perform(put("/lobby/1/customWord")
                    .header("Authorization", "Bearer validToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"word\": \"NewWord\"}"))
                    .andExpect(status().isNoContent());
        }

        @Test
        public void addCustomWord_invalidLobby_returnsNotFound() throws Exception {
            when(lobbyService.addCustomWord(eq(99L), anyString()))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

            String json = "{ \"word\": \"Tree\" }";

            mockMvc.perform(put("/lobby/99/customWord")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isNotFound());
        }

        //helper method to create player 
        private Player createPlayer(boolean ready) {
            Player player = new Player();
            player.setReady(ready);
            return player;
        }
    }
}
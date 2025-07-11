package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.Team;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.service.WebsocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.RemovePlayerDTO;
import ch.uzh.ifi.hase.soprafs24.constant.GameLanguage;

import java.util.List;

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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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
    private UserService userService;

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

        @Test
        public void addPlayerToLobby_lobbyNotFound_returns404() throws Exception {
            when(lobbyService.addPlayerToLobby(eq(1L), any(Long.class))).thenReturn(null);

            mockMvc.perform(put("/lobby/1/1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void removePlayerFromLobby_success_returnsNoContent() throws Exception {
            doNothing().when(lobbyService).removePlayerFromLobby(eq(1L), eq(1L));

            mockMvc.perform(delete("/lobby/1/1"))
                    .andExpect(status().isNoContent());

            verify(lobbyService).removePlayerFromLobby(1L, 1L);
            verify(websocketService).sendMessage(anyString(), any(RemovePlayerDTO.class));
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

        @Test
        public void getPlayerRole_playerNotFound_returns404() throws Exception {
            Lobby mockLobby = new Lobby();
            when(lobbyService.getLobbyById(1L)).thenReturn(mockLobby);

            mockMvc.perform(get("/lobby/1/role/1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void changePlayerRole_success_returnsNoContent() throws Exception {
            RoleUpdateDTO roleUpdate = new RoleUpdateDTO();
            roleUpdate.setRole("FIELD_OPERATIVE");

            Player player = new Player();
            player.setId(1L);
            player.setRole(PlayerRole.FIELD_OPERATIVE);

            when(lobbyService.changePlayerRole(eq(1L), eq(1L), eq("FIELD_OPERATIVE"))).thenReturn(player);

            mockMvc.perform(put("/lobby/1/role/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(roleUpdate)))
                    .andExpect(status().isNoContent());

            verify(lobbyService).changePlayerRole(1L, 1L, "FIELD_OPERATIVE");
        }

        @Test
        public void changePlayerRole_invalidRole_returns400() throws Exception {
            RoleUpdateDTO roleUpdate = new RoleUpdateDTO();
            roleUpdate.setRole("INVALID_ROLE");

            when(lobbyService.changePlayerRole(eq(1L), eq(1L), eq("INVALID_ROLE"))).thenReturn(null);

            mockMvc.perform(put("/lobby/1/role/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(roleUpdate)))
                    .andExpect(status().isBadRequest());
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

        @Test
        public void changePlayerTeam_invalidTeam_returns400() throws Exception {
            when(lobbyService.changePlayerTeam(eq(1L), eq(1L), eq("yellow")))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid team color"));

            String json = "{ \"color\": \"yellow\" }";

            mockMvc.perform(put("/lobby/1/team/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());
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

        @Test
        public void setPlayerReadyStatus_success_returnsNoContent() throws Exception {
            ReadyStatusDTO statusDTO = new ReadyStatusDTO();
            statusDTO.setReady(true);

            Player player = new Player();
            player.setId(1L);
            player.setReady(true);

            when(lobbyService.setPlayerReadyStatus(eq(1L), eq(1L), eq(true), any(WebsocketService.class)))
                    .thenReturn(player);

            mockMvc.perform(put("/lobby/1/status/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(statusDTO)))
                    .andExpect(status().isNoContent());

            verify(lobbyService).setPlayerReadyStatus(eq(1L), eq(1L), eq(true), any(WebsocketService.class));
        }

        @Test
        public void setPlayerReadyStatus_playerNotFound_returns404() throws Exception {
            ReadyStatusDTO statusDTO = new ReadyStatusDTO();
            statusDTO.setReady(true);

            when(lobbyService.setPlayerReadyStatus(eq(1L), eq(1L), eq(true), any(WebsocketService.class)))
                    .thenReturn(null);

            mockMvc.perform(put("/lobby/1/status/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(statusDTO)))
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

            when(lobbyService.createLobby("TestLobby", GameMode.CLASSIC,false)).thenReturn(mockLobby);

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

            when(lobbyService.getOrCreateLobby(1234, false)).thenReturn(mockLobby);

            mockMvc.perform(get("/lobby?code=1234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(99))
                    .andExpect(jsonPath("$.lobbyName").value("JoinableLobby"))
                    .andExpect(jsonPath("$.gameMode").value("CLASSIC"))
                    .andExpect(jsonPath("$.lobbyCode").value(1234));
        }

        @Test
        public void getLobbyByCode_notFound_returns404() throws Exception {
            when(lobbyService.getOrCreateLobby(1234,false)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

            mockMvc.perform(get("/lobby?code=1234"))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void getLobbyById_success() throws Exception {
            Lobby mockLobby = new Lobby();
            mockLobby.setId(1L);
            mockLobby.setLobbyName("TestLobby");
            mockLobby.setGameMode(GameMode.CLASSIC);
            mockLobby.setLobbyCode(1234);
            
            when(lobbyService.getLobbyById(1L)).thenReturn(mockLobby);
            
            mockMvc.perform(get("/lobby/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.lobbyName").value("TestLobby"))
                    .andExpect(jsonPath("$.gameMode").value("CLASSIC"))
                    .andExpect(jsonPath("$.lobbyCode").value(1234));
        }
        
        @Test
        public void getLobbyById_notFound_returns404() throws Exception {
            when(lobbyService.getLobbyById(1L)).thenReturn(null);
            
            mockMvc.perform(get("/lobby/1"))
                    .andExpect(status().isNotFound());
        }
    }
    
    @Nested
    class CustomWordHandling {
        
        @Test
        public void addCustomWord_success() throws Exception {
            Lobby mockLobby = new Lobby();
            mockLobby.setId(1L);
            mockLobby.addCustomWord("TEST");
            
            when(lobbyService.addCustomWord(eq(1L), eq("test"))).thenReturn(mockLobby);
            
            CustomWordDTO wordDTO = new CustomWordDTO();
            wordDTO.setWord("test");
            
            mockMvc.perform(put("/lobby/1/customWord")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(wordDTO)))
                    .andExpect(status().isNoContent());
            
            verify(lobbyService).addCustomWord(1L, "test");
            verify(websocketService).sendMessage(anyString(), anyList());
        }
        
        @Test
        public void addCustomWord_invalidWord_returns400() throws Exception {
            CustomWordDTO wordDTO = new CustomWordDTO();
            wordDTO.setWord("");
            
            when(lobbyService.addCustomWord(eq(1L), eq("")))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid word"));
            
            mockMvc.perform(put("/lobby/1/customWord")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(wordDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void removeCustomWord_success() throws Exception {
            Lobby mockLobby = new Lobby();
            mockLobby.setId(1L);
            mockLobby.addCustomWord("hello");

            when(lobbyService.removeCustomWord(1L, "hello")).thenReturn(mockLobby);

            CustomWordDTO wordDTO = new CustomWordDTO();
            wordDTO.setWord("hello");

            mockMvc.perform(put("/lobby/1/customWord/remove")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(wordDTO)))
                    .andExpect(status().isNoContent());

            verify(lobbyService).removeCustomWord(1L, "hello");
            verify(websocketService).sendMessage(anyString(), anyList());
        }
    }
    
    @Nested
    class GameModeHandling {
        
        @Test
        public void updateGameMode_success() throws Exception {
            Lobby mockLobby = new Lobby();
            mockLobby.setId(1L);
            mockLobby.setGameMode(GameMode.OWN_WORDS);
            
            when(lobbyService.setGameMode(eq(1L), eq(GameMode.OWN_WORDS))).thenReturn(mockLobby);
            
            mockMvc.perform(put("/lobby/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("\"OWN_WORDS\""))
                    .andExpect(status().isNoContent());
            
            verify(lobbyService).setGameMode(1L, GameMode.OWN_WORDS);
            verify(websocketService).sendMessage(anyString(), any());
        }
    }
    @Nested
    class LobbyConfigurationHandling {

        @Test
        public void setLostPlayerAccess_validRequest_returnsNoContent() throws Exception {
            String json = "{ \"openForLostPlayers\": true }";

            mockMvc.perform(put("/lobby/1/lost-players")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isNoContent());

            verify(lobbyService).setOpenForLostPlayers(1L, true);
            verify(websocketService).sendMessage("/topic/lobby/1/lostPlayers", true);
        }

        @Test
        public void getJoinableLobby_success() throws Exception {
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            lobby.setLobbyName("Joinable");
            lobby.setGameMode(GameMode.CLASSIC);
            lobby.setLobbyCode(1234);

            when(lobbyService.getAllJoinableLobbies()).thenReturn(List.of(lobby));

            mockMvc.perform(get("/lobby/lost"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.lobbyName").value("Joinable"))
                    .andExpect(jsonPath("$.gameMode").value("CLASSIC"));
        }

        @Test
        public void getLobbyTheme_returnsDefaultTheme() throws Exception {
            Lobby lobby = new Lobby();
            lobby.setTheme(null);
            when(lobbyService.getLobbyById(1L)).thenReturn(lobby);

            mockMvc.perform(get("/lobby/1/theme"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.theme").value("default"));
        }

        @Test
        public void setLobbyTheme_validRequest_returnsNoContent() throws Exception {
            String json = "{ \"theme\": \"sci-fi\" }";

            mockMvc.perform(put("/lobby/1/theme")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isNoContent());

            verify(lobbyService).setTheme(1L, "sci-fi");
            verify(websocketService).sendMessage("/topic/lobby/1/theme", "sci-fi");
        }

        @Test
        public void setLobbyLanguage_validRequest_returnsNoContent() throws Exception {
            mockMvc.perform(put("/lobby/1/language")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"ENGLISH\""))
                    .andExpect(status().isNoContent());

            verify(lobbyService).setLanguage(1L, GameLanguage.ENGLISH);
            verify(websocketService).sendMessage("/topic/lobby/1/language", GameLanguage.ENGLISH);
        }
    }

    @Nested
    class TurnDurationHandling {

        @Test
        public void setTurnDuration_validInput_turnDurationUpdated() throws Exception {
            Lobby mockLobby = new Lobby();
            mockLobby.setId(1L);
            mockLobby.setTurnDuration(45);
            mockLobby.setGameMode(GameMode.CLASSIC);

            when(lobbyService.setTurnDuration(1L, 45)).thenReturn(mockLobby);
            when(lobbyService.getLobbyById(1L)).thenReturn(mockLobby);

            // Set the turn duration
            mockMvc.perform(put("/lobby/1/turnDuration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("45"))
                    .andExpect(status().isNoContent());

            // Retrieve the lobby to confirm
            mockMvc.perform(get("/lobby/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.turnDuration").value(45));
        }
    }

    @Test
    public void countPlayersLobby_returnsPlayerStatus() throws Exception {
        LobbyPlayersResponseDTO dto = new LobbyPlayersResponseDTO(4, 2, List.of());
        dto.setTotalPlayers(4);

        when(lobbyService.sendLobbyPlayerStatusUpdate(1L)).thenReturn(dto);

        mockMvc.perform(get("/lobby/1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPlayers").value(4));

        verify(lobbyService).sendLobbyPlayerStatusUpdate(1L);
    }

    @Test
    public void getCustomWords_success_returnsList() throws Exception {
        Lobby mockLobby = new Lobby();
        mockLobby.addCustomWord("alpha");
        mockLobby.addCustomWord("beta");

        when(lobbyService.getLobbyById(1L)).thenReturn(mockLobby);

        mockMvc.perform(get("/lobby/1/customWords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("ALPHA"))
                .andExpect(jsonPath("$[1]").value("BETA"));

        verify(lobbyService).getLobbyById(1L);
    }

    @Test
    public void sendChatMessage_teamChat_success() throws Exception {
        var user = new User();
        user.setId(1L);
        user.setUsername("testUser");

        when(userService.extractToken(any())).thenReturn("token");
        when(userService.validateToken("token")).thenReturn(user);
        when(lobbyService.getTeamColorByPlayer(1L)).thenReturn(TeamColor.RED);

        mockMvc.perform(post("/lobby/1/chat")
                        .header("Authorization", "Bearer token")
                        .param("chatType", "team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"Hello team!\""))
                .andExpect(status().isNoContent());

        verify(websocketService).sendMessage(contains("/topic/lobby/1/chat/team/RED"), any());
    }

    @Test
    public void sendChatMessage_globalChat_success() throws Exception {
        var user = new User();
        user.setId(2L);
        user.setUsername("globalUser");

        when(userService.extractToken(any())).thenReturn("token2");
        when(userService.validateToken("token2")).thenReturn(user);
        when(lobbyService.getTeamColorByPlayer(2L)).thenReturn(TeamColor.BLUE);

        mockMvc.perform(post("/lobby/1/chat")
                        .header("Authorization", "Bearer token2")
                        .param("chatType", "global")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"Global message!\""))
                .andExpect(status().isNoContent());

        verify(websocketService).sendMessage(contains("/topic/lobby/1/chat/global"), any());
    }

    @Test
    public void sendChatMessage_invalidChatType_returns400() throws Exception {
        var user = new User();
        user.setId(3L);
        user.setUsername("badUser");

        when(userService.extractToken(any())).thenReturn("token3");
        when(userService.validateToken("token3")).thenReturn(user);
        when(lobbyService.getTeamColorByPlayer(3L)).thenReturn(TeamColor.BLUE);

        mockMvc.perform(post("/lobby/1/chat")
                        .header("Authorization", "Bearer token3")
                        .param("chatType", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"oops\""))
                .andExpect(status().isBadRequest());
    }

    
}
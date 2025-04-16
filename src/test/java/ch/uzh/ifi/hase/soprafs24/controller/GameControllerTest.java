package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameStartDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GiveHintDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.makeGuessDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.service.WebsocketService;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(GameController.class)
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    @MockBean
    private WebsocketService webSocketService;
    
    @MockBean
    private UserService userService;
    
    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class HintHandling {
        
        @Test
        public void giveHint_success_returnsNoContent() throws Exception {
            // Create test data
            GiveHintDTO hintDTO = new GiveHintDTO();
            hintDTO.setHint("ocean");
            hintDTO.setWordsCount(3);
            
            User mockUser = new User();
            mockUser.setId(1L);
            
            // Configure mocks
            when(userService.extractToken(anyString())).thenReturn("valid-token");
            when(userService.validateToken("valid-token")).thenReturn(mockUser);
            
            // Perform the request
            mockMvc.perform(put("/game/1/hint")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer valid-token")
                    .content(asJsonString(hintDTO)))
                    .andExpect(status().isNoContent());
            
            // Verify that service methods were called correctly
            verify(userService).extractToken("Bearer valid-token");
            verify(userService).validateToken("valid-token");
            verify(gameService).checkIfUserSpymaster(mockUser);
            verify(gameService).validateHint("ocean", 3, 1L);
        }
        
        @Test
        public void giveHint_notSpymaster_returns403() throws Exception {
            // Create test data
            GiveHintDTO hintDTO = new GiveHintDTO();
            hintDTO.setHint("ocean");
            hintDTO.setWordsCount(3);
            
            User mockUser = new User();
            mockUser.setId(1L);
            
            // Configure mocks
            when(userService.extractToken(anyString())).thenReturn("valid-token");
            when(userService.validateToken("valid-token")).thenReturn(mockUser);
            doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only spymasters can give hints"))
                .when(gameService).checkIfUserSpymaster(mockUser);
            
            // Perform the request
            mockMvc.perform(put("/game/1/hint")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer valid-token")
                    .content(asJsonString(hintDTO)))
                    .andExpect(status().isForbidden());
        }
        
        @Test
        public void giveHint_invalidHint_returns400() throws Exception {
            // Create test data
            GiveHintDTO hintDTO = new GiveHintDTO();
            hintDTO.setHint("");
            hintDTO.setWordsCount(3);
            
            User mockUser = new User();
            mockUser.setId(1L);
            
            // Configure mocks
            when(userService.extractToken(anyString())).thenReturn("valid-token");
            when(userService.validateToken("valid-token")).thenReturn(mockUser);
            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hint cannot be empty"))
                .when(gameService).validateHint("", 3, 1L);
            
            // Perform the request
            mockMvc.perform(put("/game/1/hint")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer valid-token")
                    .content(asJsonString(hintDTO)))
                    .andExpect(status().isBadRequest());
        }
    }  
    
    @Nested
    class GameStartHandling {
        
        @Test
        public void startGame_success_returnsGame() throws Exception {
            // Create test data
            GameStartDTO gameStartDTO = new GameStartDTO();
            gameStartDTO.setStartingTeam(TeamColor.RED);
            gameStartDTO.setGameMode(GameMode.CLASSIC);
            gameStartDTO.setTheme("default");
            
            Game mockGame = new Game();
            mockGame.setId(1L);
            mockGame.setStartingTeam(TeamColor.RED);
            mockGame.setGameMode(GameMode.CLASSIC);
            
            // Configure mocks
            when(gameService.startOrGetGame(1L, TeamColor.RED, GameMode.CLASSIC))
                .thenReturn(mockGame);
            
            // Perform the request
            mockMvc.perform(post("/game/1/start")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(gameStartDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.startingTeam").value("RED"))
                    .andExpect(jsonPath("$.gameMode").value("CLASSIC"));
        }
    }
    
    @Nested
    class GameBoardHandling {
        
        @Test
        public void getBoard_returnsCardList() throws Exception {
            // Create test data
            List<Card> mockCards = new ArrayList<>();
            Card card1 = new Card();
            card1.setWord("apple");
            Card card2 = new Card();
            card2.setWord("banana");
            mockCards.add(card1);
            mockCards.add(card2);
            
            // Configure mocks
            when(gameService.getBoard(1L)).thenReturn(mockCards);
            
            // Perform the request
            mockMvc.perform(get("/game/1/board"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].word").value("apple"))
                    .andExpect(jsonPath("$[1].word").value("banana"));
            
            // Verify that service method was called correctly
            verify(gameService).getBoard(1L);
        }
        
        @Test
        public void getBoard_gameNotFound_returns404() throws Exception {
            // Configure mocks
            when(gameService.getBoard(1L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
            
            // Perform the request
            mockMvc.perform(get("/game/1/board"))
                    .andExpect(status().isNotFound());
        }
    }
    
    @Nested
    class GuessHandling {
        
        @Test
        public void makeGuess_success_returnsNoContent() throws Exception {
            // Create test data
            makeGuessDTO guessDTO = new makeGuessDTO();
            guessDTO.setTeamColor("RED");
            guessDTO.setWordStr("apple");
            
            User mockUser = new User();
            mockUser.setId(1L);
            
            // Configure mocks
            when(userService.extractToken(anyString())).thenReturn("valid-token");
            when(userService.validateToken("valid-token")).thenReturn(mockUser);
            when(gameService.makeGuess(eq(1L), eq(TeamColor.RED), eq("apple"), any(User.class)))
                .thenReturn(Map.entry(false, TeamColor.BLUE));
            
            // Perform the request
            mockMvc.perform(put("/game/1/guess")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer valid-token")
                    .content(asJsonString(guessDTO)))
                    .andExpect(status().isNoContent());
            
            // Verify that service and websocket methods were called correctly
            verify(userService).extractToken("Bearer valid-token");
            verify(userService).validateToken("valid-token");
            verify(gameService).makeGuess(eq(1L), eq(TeamColor.RED), eq("apple"), any(User.class));
        }
        
        @Test
        public void makeGuess_gameOver_returnsNoContent() throws Exception {
            // Create test data
            makeGuessDTO guessDTO = new makeGuessDTO();
            guessDTO.setTeamColor("RED");
            guessDTO.setWordStr("apple");
            
            User mockUser = new User();
            mockUser.setId(1L);
            
            // Configure mocks
            when(userService.extractToken(anyString())).thenReturn("valid-token");
            when(userService.validateToken("valid-token")).thenReturn(mockUser);
            when(gameService.makeGuess(eq(1L), eq(TeamColor.RED), eq("apple"), any(User.class)))
                .thenReturn(Map.entry(true, TeamColor.RED));
            
            // Perform the request
            mockMvc.perform(put("/game/1/guess")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer valid-token")
                    .content(asJsonString(guessDTO)))
                    .andExpect(status().isNoContent());
            
            // Verify that websocket game completed message was sent
            verify(webSocketService).sendMessage("/topic/game/1/gameCompleted", "RED");
            verify(gameService).updatePlayerStats(1L, TeamColor.RED);
        }
        
        @Test
        public void makeGuess_invalidTeamColor_returns400() throws Exception {
            // Create test data
            makeGuessDTO guessDTO = new makeGuessDTO();
            guessDTO.setTeamColor("YELLOW");
            guessDTO.setWordStr("apple");
            
            // Configure mocks
            when(userService.extractToken(anyString())).thenReturn("valid-token");
            when(userService.validateToken("valid-token")).thenReturn(new User());
            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid team color: YELLOW"))
                .when(gameService).makeGuess(anyLong(), any(), anyString(), any(User.class));
            
            // Perform the request
            mockMvc.perform(put("/game/1/guess")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer valid-token")
                    .content(asJsonString(guessDTO)))
                    .andExpect(status().isBadRequest());
        }
        
        @Test
        public void makeGuess_notYourTurn_returns403() throws Exception {
            // Create test data
            makeGuessDTO guessDTO = new makeGuessDTO();
            guessDTO.setTeamColor("RED");
            guessDTO.setWordStr("apple");
            
            // Configure mocks
            when(userService.extractToken(anyString())).thenReturn("valid-token");
            when(userService.validateToken("valid-token")).thenReturn(new User());
            doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "It's not your turn"))
                .when(gameService).makeGuess(anyLong(), any(), anyString(), any(User.class));
            
            // Perform the request
            mockMvc.perform(put("/game/1/guess")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer valid-token")
                    .content(asJsonString(guessDTO)))
                    .andExpect(status().isForbidden());
        }
    }
}

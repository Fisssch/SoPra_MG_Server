package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameStartDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.service.WebsocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    private User testUser;

    @BeforeEach
    public void setup() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
    }

    //helper method 
    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getGameWords_gameNotFound_returnsNotFound() throws Exception {
        Game dummyGame = new Game(); 
        dummyGame.setId(1L);
        given(userService.validateToken(Mockito.any())).willReturn(testUser);
        given(gameService.generateWords(Mockito.eq(dummyGame), Mockito.eq("default")))
                .willThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        MockHttpServletRequestBuilder getRequest = get("/game/1/words")
                .header("Authorization", "Bearer validToken")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    public void startGame_validRequest_returnsGame() throws Exception {
        Game game = new Game();
        game.setId(1L);

        GameStartDTO gameStartDTO = new GameStartDTO();
        gameStartDTO.setStartingTeam(TeamColor.RED);
        gameStartDTO.setGameMode(GameMode.CLASSIC);
        gameStartDTO.setTheme("default");

        given(userService.validateToken(Mockito.any())).willReturn(testUser);
        given(gameService.startOrGetGame(Mockito.eq(1L), Mockito.any(), Mockito.any(), Mockito.any()))
                .willReturn(game);

        MockHttpServletRequestBuilder postRequest = post("/game/1/start")
                .header("Authorization", "Bearer validToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gameStartDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    public void startGame_gameCreationFails_returnsBadRequest() throws Exception {
        GameStartDTO gameStartDTO = new GameStartDTO();
        gameStartDTO.setStartingTeam(TeamColor.RED);
        gameStartDTO.setGameMode(GameMode.CLASSIC);
        gameStartDTO.setTheme("default");

        given(userService.validateToken(Mockito.any())).willReturn(testUser);
        given(gameService.startOrGetGame(Mockito.eq(1L), Mockito.any(), Mockito.any(), Mockito.any()))
                .willThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST));

        MockHttpServletRequestBuilder postRequest = post("/game/1/start")
                .header("Authorization", "Bearer validToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gameStartDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getBoard_validRequest_returnsBoard() throws Exception {
        List<Card> board = List.of(
                new Card("Word1", null),
                new Card("Word2", null)
        );

        given(userService.validateToken(Mockito.any())).willReturn(testUser);
        given(gameService.getBoard(Mockito.eq(1L))).willReturn(board);

        MockHttpServletRequestBuilder getRequest = get("/game/1/board")
                .header("Authorization", "Bearer validToken")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].word", is("Word1")))
                .andExpect(jsonPath("$[1].word", is("Word2")));
    }

    @Test
    public void getBoard_gameNotFound_returnsNotFound() throws Exception {
        given(userService.validateToken(Mockito.any())).willReturn(testUser);
        given(gameService.getBoard(Mockito.eq(1L)))
                .willThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        MockHttpServletRequestBuilder getRequest = get("/game/1/board")
                .header("Authorization", "Bearer validToken")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

}

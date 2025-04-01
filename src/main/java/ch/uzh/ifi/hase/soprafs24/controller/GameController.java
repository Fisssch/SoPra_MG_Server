package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameStartDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.service.WebsocketService;
import ch.uzh.ifi.hase.soprafs24.service.WordGenerationService;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;


@CrossOrigin(
  origins = "http://localhost:3000", 
  exposedHeaders = "Authorization"
) 
@RestController
public class GameController {  
    private final GameService gameService;
    private final WebsocketService webSocketService;
    private final UserService userService;
    private final WordGenerationService wordGenerationService;
    
    GameController(GameService gameService, WebsocketService webSocketService, UserService userService, WordGenerationService wordGenerationService) {
        this.gameService = gameService;
        this.webSocketService = webSocketService;
        this.userService = userService;
        this.wordGenerationService = wordGenerationService;
    }

    @GetMapping("/game/{id}/words")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getGameWords(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        userService.extractAndValidateToken(authHeader);
        List<String> words= gameService.generateWords(id, "default"); //call here with default since we never create new words here, just get current words from game 
        return words;
    } 

    @PostMapping("/game/{id}/start")
    @ResponseStatus(HttpStatus.OK)
    public Game startGame(@PathVariable Long id,
                      @RequestHeader("Authorization") String authHeader,
                      @RequestBody GameStartDTO gameStartDTO) {
        userService.extractAndValidateToken(authHeader);

        TeamColor startingTeam = gameStartDTO.getStartingTeam();
        GameMode gameMode = gameStartDTO.getGameMode();
        String theme = gameStartDTO.getTheme();
        Game game = gameService.startOrGetGame(id, startingTeam, gameMode, theme);
        return game;
        }

    @GetMapping("/game/{id}/board")
    @ResponseStatus(HttpStatus.OK)
    public List<Card> getBoard(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        userService.extractAndValidateToken(authHeader);
        return gameService.getBoard(id);
    }

}
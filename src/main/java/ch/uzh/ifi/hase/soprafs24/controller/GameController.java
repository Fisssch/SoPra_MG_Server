package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.annotation.AuthorizationRequired;
import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameStartDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GiveHintDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.makeGuessDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.service.WebsocketService;

@RestController
public class GameController {  
    private final GameService gameService;
    private final WebsocketService webSocketService;
    private final UserService userService;
    
    GameController(GameService gameService, WebsocketService webSocketService, UserService userService) {
        this.gameService = gameService;
        this.webSocketService = webSocketService;
        this.userService = userService;
    }

    @PutMapping("game/{id}/hint")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void giveHint(@PathVariable Long id, @RequestHeader("Authorization") String authHeader, @RequestBody GiveHintDTO hint) {
        String token = userService.extractToken(authHeader);
        gameService.checkIfUserSpymaster(userService.validateToken(token));
        gameService.validateHint(hint.getHint(), hint.getWordsCount(), id);
        webSocketService.sendMessage("/topic/game/" + id + "/hint", hint);
    }

    //@GetMapping("/game/{id}/words")
    //@ResponseStatus(HttpStatus.OK)
    //@AuthorizationRequired
    //public List<String> getGameWords(@PathVariable Long id) {
    //    List<String> words= gameService.generateWords(id, "default"); //call here with default since we never create new words here, just get current words from game 
    //    return words;
    //} 

    @PostMapping("/game/{id}/start")
    @ResponseStatus(HttpStatus.OK)
    @AuthorizationRequired
    public Game startGame(@PathVariable Long id,
                      @RequestBody GameStartDTO gameStartDTO) {
        TeamColor startingTeam = gameStartDTO.getStartingTeam();
        GameMode gameMode = gameStartDTO.getGameMode();
        String theme = gameStartDTO.getTheme();
        return gameService.startOrGetGame(id, startingTeam, gameMode, theme);
    }

    @GetMapping("/game/{id}/board")
    @ResponseStatus(HttpStatus.OK)
    @AuthorizationRequired
    public List<Card> getBoard(@PathVariable Long id) {
        return gameService.getBoard(id);
    }

    @PutMapping("/game/{id}/guess")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void makeGuess(@PathVariable Long id, @RequestHeader("Authorization") String authHeader, @RequestBody makeGuessDTO guessDTO) {
        String token = userService.extractToken(authHeader);
        String colorStr = guessDTO.getTeamColor();
        TeamColor color;
        try{
            color = TeamColor.valueOf(colorStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid team color: " + colorStr);
        }
        var result = gameService.makeGuess(id, color, guessDTO.getWordStr(), userService.validateToken(token));
        var isGameCompleted = result.getKey();
        var team = result.getValue();
        if (Boolean.TRUE.equals(isGameCompleted)) {
            webSocketService.sendMessage("/topic/game/" + id + "/gameCompleted", team.name());
            gameService.updatePlayerStats(id, team);
        } else {
            webSocketService.sendMessage("/topic/game/" + id + "/guess", guessDTO);
        }
        // Send the updated board to all clients
        List<Card> updatedBoard = gameService.getBoard(id);
        webSocketService.sendMessage("/topic/game/" + id + "/board", updatedBoard);
    } 
}

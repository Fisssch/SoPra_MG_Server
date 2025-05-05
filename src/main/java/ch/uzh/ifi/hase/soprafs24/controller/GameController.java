package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameStartDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GiveHintDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.SelectWordDTO;
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
        Map<String, Object> payload = new HashMap<>();
        payload.put("hint", hint.getHint());
        payload.put("wordsCount", hint.getWordsCount());
        payload.put("guessesLeft", hint.getWordsCount()); // Initialize guessesLeft to the number of words in the hint
    
        // Send the payload to the WebSocket
        webSocketService.sendMessage("/topic/game/" + id + "/hint", payload);
    }
    

    //@GetMapping("/game/{id}/words")
    //@ResponseStatus(HttpStatus.OK)
    //@AuthorizationRequired
    //public List<String> getGameWords(@PathVariable Long id) {
    //    List<String> words= gameService.generateWords(id, "default"); //call here with default since we never create new words here, just get current words from game 
    //    return words;
    //} 
    
    @PutMapping("/game/{id}/endTurn")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void endTurn(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        String token = userService.extractToken(authHeader);
        gameService.endTurn(id, userService.validateToken(token));
    
        // Notify all clients about the updated turn
        Game game = gameService.getGameById(id);
        Map<String, Object> payload = new HashMap<>();
        payload.put("teamTurn", game.getTeamTurn().name()); // Only include the updated team turn
    
        // Send the payload to a dedicated WebSocket topic
        webSocketService.sendMessage("/topic/game/" + id + "/turn", payload);
    }
    @PostMapping("/game/{id}/start")
    @ResponseStatus(HttpStatus.OK)
    @AuthorizationRequired
    public Game startGame(@PathVariable Long id,
                      @RequestBody GameStartDTO gameStartDTO) {
        TeamColor startingTeam = gameStartDTO.getStartingTeam();
        GameMode gameMode = gameStartDTO.getGameMode();
        return gameService.startOrGetGame(id, startingTeam, gameMode);
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
            //webSocketService.sendMessage("/topic/lobby/" + id + "/end", true); commenting out, sicne no need for it right now 
            gameService.updatePlayerStats(id, team);
        } else {
            guessDTO.setTeamColor(team.name());
            webSocketService.sendMessage("/topic/game/" + id + "/guess", guessDTO);
        }
        // Send the updated board to all clients
        List<Card> updatedBoard = gameService.getBoard(id);
        int guessesLeft = gameService.getRemainingGuesses(id);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("updatedBoard", updatedBoard);
        payload.put("guessesLeft", guessesLeft);

        webSocketService.sendMessage("/topic/game/" + id + "/board", payload);
    } 

    @PutMapping("/game/{id}/selectWord")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void selectWord(@PathVariable Long id, @RequestHeader("Authorization") String authHeader, @RequestBody SelectWordDTO selectWordDTO) {
        String token = userService.extractToken(authHeader);
        User user = userService.validateToken(token); 
        TeamColor teamColor = TeamColor.valueOf(selectWordDTO.getTeamColor().toUpperCase());

        // Validate if the user is a field operative for the selected team
        gameService.checkIfUserIsFieldOperative(user.getId(), teamColor);

        // Call the service to process the word selection
        gameService.selectWord(id, selectWordDTO);

        // Send the updated board to all players via WebSocket
        List<Card> updatedBoard = gameService.getBoard(id);
        int guessesLeft = gameService.getRemainingGuesses(id);

        Map<String, Object> payload = new HashMap<>();
        payload.put("updatedBoard", updatedBoard); // Send the updated board
        payload.put("guessesLeft", guessesLeft); 
        
        // Send the selection to the WebSocket topic
        webSocketService.sendMessage("/topic/game/" + id + "/board", payload); 
    }

    
}

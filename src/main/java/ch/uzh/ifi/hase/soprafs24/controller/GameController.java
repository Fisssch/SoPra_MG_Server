package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

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

    ///@PutMapping("game/{id}/hint")
    ///@ResponseStatus(HttpStatus.NO_CONTENT)
    ///public void giveHint(@PathVariable Long id, @RequestHeader("Authorization") String authHeader, @RequestBody GiveHintDTO hint) {
    ///    userService.extractAndValidateToken(authHeader); 
    ///    gameService.validateHint(hint.getHint(), hint.getWordsCount(), hint.getTeamId(), id);
    ///    webSocketService.sendMessage("/topic/game/" + id + "/hint/", hint);
    ///}

    @GetMapping("game/{id}/words")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getGameWords(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        userService.extractAndValidateToken(authHeader);
        List<String> words= gameService.generateWords(id); 
        return words;
    }
}
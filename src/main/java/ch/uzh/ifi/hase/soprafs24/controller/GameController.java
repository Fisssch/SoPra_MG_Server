package ch.uzh.ifi.hase.soprafs24.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs24.rest.dto.GiveHintDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.service.WebsocketService;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;

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
        userService.extractAndValidateToken(authHeader); 
        gameService.validateHint(hint.getHint(), hint.getWordsCount(), hint.getTeamId(), id);
        webSocketService.sendMessage("/topic/game/" + id + "/hint/", hint);
    }
}

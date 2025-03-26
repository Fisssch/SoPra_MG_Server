package ch.uzh.ifi.hase.soprafs24.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GetLobbyDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.WebsocketService;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class LobbyController {  
    private final LobbyService lobbyService;
    private final WebsocketService webSocketService;
    
    LobbyController(LobbyService lobbyService, WebsocketService webSocketService) {
        this.lobbyService = lobbyService;
        this.webSocketService = webSocketService;
    }

    @GetMapping("lobby")
    @ResponseStatus(HttpStatus.CREATED)
    public GetLobbyDTO getOrCreateLobby(/* token */) {
        return DTOMapper.INSTANCE.convertEntitytoGetLobbyDTO(lobbyService.getOrCreateLobby());
    }
    
    @PutMapping("lobby/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putMethodName(@PathVariable Integer id, @RequestBody GameMode gameMode) {
        var lobby = lobbyService.setGameMode(id, gameMode);
        webSocketService.sendMessage("/topic/lobby", DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby));
    }
}

package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.annotation.AuthorizationRequired;
import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.WebsocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.*;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/lobby")
public class LobbyController {

    private final LobbyService lobbyService;
    private final WebsocketService webSocketService;

    LobbyController(LobbyService lobbyService, WebsocketService webSocketService) {
        this.lobbyService = lobbyService;
        this.webSocketService = webSocketService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @AuthorizationRequired
    public LobbyResponseDTO getOrCreateLobby(@RequestParam(required = false) Integer code) {
        Lobby lobby;
        lobby = lobbyService.getOrCreateLobby(code);

        return new LobbyResponseDTO(
                lobby.getId(),
                lobby.getLobbyName(),
                lobby.getGameMode().name(),
                lobby.getLobbyCode(),
                lobby.getCreatedAt()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AuthorizationRequired
    public LobbyResponseDTO createLobby(@RequestBody LobbyPostDTO lobbyPostDTO) {
        GameMode mode;
        try {
            mode = GameMode.valueOf(lobbyPostDTO.getGameMode().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or missing game mode");
        }

        Lobby lobby = lobbyService.createLobby(lobbyPostDTO.getLobbyName(), mode);

        return new LobbyResponseDTO(
                lobby.getId(),
                lobby.getLobbyName(),
                lobby.getGameMode().name(),
                lobby.getLobbyCode(),
                lobby.getCreatedAt()
        );
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @AuthorizationRequired
    public LobbyResponseDTO getLobbyById(@PathVariable Long id) {
        Lobby lobby = lobbyService.getLobbyById(id);
        if (lobby == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
        }

        lobbyService.scheduleLobbyTimeout(lobby);

        return new LobbyResponseDTO(
                lobby.getId(),
                lobby.getLobbyName(),
                lobby.getGameMode().name(),
                lobby.getLobbyCode(),
                lobby.getCreatedAt()
        );
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AuthorizationRequired
    public void updateGameMode(@PathVariable Long id, @RequestBody GameMode gameMode) {
        var lobby = lobbyService.setGameMode(id, gameMode);
        webSocketService.sendMessage("/topic/lobby/" + id + "/gameMode", DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby));
    }

    @PutMapping("/{id}/theme")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AuthorizationRequired
    public void setLobbyTheme(@PathVariable Long id, @RequestBody ThemeDTO themeDTO) {
        lobbyService.setTheme(id, themeDTO.getTheme());
        webSocketService.sendMessage("/topic/lobby/" + id + "/theme", themeDTO.getTheme());
    }

    @GetMapping("/{id}/theme")
    @ResponseStatus(HttpStatus.OK)
    @AuthorizationRequired
    public ThemeDTO getLobbyTheme(@PathVariable Long id) {
        Lobby lobby = lobbyService.getLobbyById(id);

        ThemeDTO themeDTO = new ThemeDTO();
        themeDTO.setTheme(lobby.getTheme() != null ? lobby.getTheme() : "default");
        return themeDTO;
    }

    @PutMapping("/{id}/{playerId}")
    @ResponseStatus(HttpStatus.OK)
    @AuthorizationRequired
    public PlayerResponseDTO addPlayerToLobby(@PathVariable Long id, @PathVariable Long playerId) {
        Player addedPlayer = lobbyService.addPlayerToLobby(id, playerId);

        if (addedPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby or player not found");
        }

        PlayerResponseDTO response = new PlayerResponseDTO();
        response.setId(addedPlayer.getId());
        response.setRole(addedPlayer.getRole() != null ? addedPlayer.getRole().name() : null);
        response.setReady(addedPlayer.getReady());
        if (addedPlayer.getTeam() != null) {
            response.setTeamColor(addedPlayer.getTeam().getColor().name());
        }

        PlayerUpdateDTO updateDto = DTOMapper.INSTANCE.convertEntityToPlayerUpdateDTO(addedPlayer);
        updateDto.setColor(addedPlayer.getTeam() != null ? addedPlayer.getTeam().getColor().name() : null);
        updateDto.setRole(addedPlayer.getRole() != null ? addedPlayer.getRole().name() : null);

        webSocketService.sendMessage("/topic/lobby/" + id + "/addPlayer", updateDto);

        return response;
    }

    @DeleteMapping("/{id}/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AuthorizationRequired
    public void removePlayerFromLobby(@PathVariable Long id, @PathVariable Long playerId) {   
        lobbyService.removePlayerFromLobby(id, playerId);

        webSocketService.sendMessage("/topic/lobby/" + id + "/removePlayer", new RemovePlayerDTO(playerId));
    }

    @GetMapping("/{id}/role/{playerId}")
    @ResponseStatus(HttpStatus.OK)
    @AuthorizationRequired
    public PlayerRoleDTO getPlayerRole(@PathVariable Long id, @PathVariable Long playerId) {
        Lobby lobby = lobbyService.getLobbyById(id);
        if (lobby == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");

        Player player = lobby.getPlayers().stream()
                .filter(p -> playerId.equals(p.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));


                return new PlayerRoleDTO(player.getRole() != null ? player.getRole().name() : null);
    }

    /**
     * Changes the role of a player in a given lobby.
     * Throws 400 if role is invalid or player not found.
     * Throws 409 if the team already has a spymaster.
     */
    @PutMapping("/{id}/role/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AuthorizationRequired
    public void changePlayerRole(@PathVariable Long id, @PathVariable Long playerId, @RequestBody RoleUpdateDTO roleUpdate) {
        Player updatedPlayer = lobbyService.changePlayerRole(id, playerId, roleUpdate.getRole());
        if (updatedPlayer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role or player not found");
        }
        PlayerUpdateDTO playerUpdateDTO = DTOMapper.INSTANCE.convertEntityToPlayerUpdateDTO(updatedPlayer);
        playerUpdateDTO.setColor(updatedPlayer.getTeam() != null ? updatedPlayer.getTeam().getColor().name() : null);
        playerUpdateDTO.setRole(updatedPlayer.getRole() != null ? updatedPlayer.getRole().name() : null);
        webSocketService.sendMessage("/topic/lobby/" + id + "/players", playerUpdateDTO);
    }

    @GetMapping("/{id}/team/{playerId}")
    @ResponseStatus(HttpStatus.OK)
    @AuthorizationRequired
    public PlayerTeamDTO getPlayerTeam(@PathVariable Long id, @PathVariable Long playerId) {
        Lobby lobby = lobbyService.getLobbyById(id);
        if (lobby == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");

        Player player = lobby.getPlayers().stream()
                .filter(p -> playerId.equals(p.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        if (player.getTeam() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not assigned");
        }

        return new PlayerTeamDTO(player.getTeam().getColor().name());
    }

    @PutMapping("/{id}/team/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AuthorizationRequired
    public void changePlayerTeam(@PathVariable Long id, @PathVariable Long playerId, @RequestBody TeamUpdateDTO teamUpdate) {
        Player updatedPlayer = lobbyService.changePlayerTeam(id, playerId, teamUpdate.getColor());
        if (updatedPlayer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid team color or player not found");
        }
        PlayerUpdateDTO playerUpdateDTO = DTOMapper.INSTANCE.convertEntityToPlayerUpdateDTO(updatedPlayer);
        playerUpdateDTO.setColor(updatedPlayer.getTeam() != null ? updatedPlayer.getTeam().getColor().name() : null);
        playerUpdateDTO.setRole(updatedPlayer.getRole() != null ? updatedPlayer.getRole().name() : null);
        webSocketService.sendMessage("/topic/lobby/" + id + "/players", playerUpdateDTO);
    }

    @GetMapping("/{id}/status/{playerId}")
    @ResponseStatus(HttpStatus.OK)
    @AuthorizationRequired
    public ReadyStatusDTO getPlayerReadyStatus(@PathVariable Long id, @PathVariable Long playerId) {
        Boolean ready = lobbyService.getPlayerReadyStatus(id, playerId);
        if (ready == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player or lobby not found");
        }

        ReadyStatusDTO dto = new ReadyStatusDTO();
        dto.setReady(ready);
        return dto;
    }

    @PutMapping("/{id}/status/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AuthorizationRequired
    public void setPlayerReadyStatus(@PathVariable Long id, @PathVariable Long playerId, @RequestBody ReadyStatusDTO statusUpdate) {
        Player updatedPlayer = lobbyService.setPlayerReadyStatus(id, playerId, statusUpdate.getReady(), webSocketService);
        if (updatedPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby or player not found");
        }
        PlayerUpdateDTO playerUpdateDTO = DTOMapper.INSTANCE.convertEntityToPlayerUpdateDTO(updatedPlayer);
        playerUpdateDTO.setColor(updatedPlayer.getTeam() != null ? updatedPlayer.getTeam().getColor().name() : null);
        playerUpdateDTO.setRole(updatedPlayer.getRole() != null ? updatedPlayer.getRole().name() : null);
        webSocketService.sendMessage("/topic/lobby/" + id + "/players", playerUpdateDTO);
    }

    @PutMapping("/{id}/customWord")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AuthorizationRequired
    public void addCustomWord(@PathVariable Long id, @RequestBody CustomWordDTO wordDTO){
        Lobby updatedLobby = lobbyService.addCustomWord(id, wordDTO.getWord());
        webSocketService.sendMessage("/topic/lobby/" + id + "/customWords", updatedLobby.getCustomWords());
    }

    @PutMapping("/{id}/customWord/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AuthorizationRequired
    public void removeCustomWord(@PathVariable Long id, @RequestBody CustomWordDTO wordDTO) {
        Lobby updatedLobby = lobbyService.removeCustomWord(id, wordDTO.getWord());
        webSocketService.sendMessage("/topic/lobby/" + id + "/customWords", updatedLobby.getCustomWords());
    }

    @GetMapping("/{id}/players")
    @ResponseStatus(HttpStatus.OK)
    @AuthorizationRequired
    public LobbyPlayersResponseDTO countPlayersLobby(@PathVariable Long id) {
        return lobbyService.sendLobbyPlayerStatusUpdate(id);
    }

    @GetMapping("/{id}/customWords")
    @ResponseStatus(HttpStatus.OK)
    @AuthorizationRequired
    public List<String> getCustomWords(@PathVariable Long id) {
        Lobby lobby = lobbyService.getLobbyById(id);
        return lobby.getCustomWords();
    }

}
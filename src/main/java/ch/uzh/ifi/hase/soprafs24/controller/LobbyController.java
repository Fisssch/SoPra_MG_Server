package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.WebsocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.PlayerUpdateDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.RemovePlayerDTO;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
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
                lobby.getGameMode().name()
        );
    }

    @PutMapping("/{id}/{playerId}")
    @ResponseStatus(HttpStatus.OK)
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
            response.setTeamColor(addedPlayer.getTeam().getColor());
        }

        PlayerUpdateDTO updateDto = DTOMapper.INSTANCE.convertEntityToPlayerUpdateDTO(addedPlayer);
        updateDto.setColor(addedPlayer.getTeam() != null ? addedPlayer.getTeam().getColor() : null);
        updateDto.setRole(addedPlayer.getRole() != null ? addedPlayer.getRole().name() : null);

        webSocketService.sendMessage("/topic/lobby" + id + "/addPlayer", updateDto);

        return response;
    }

    @DeleteMapping("/{id}/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePlayerFromLobby(@PathVariable Long id, @PathVariable Long playerId) {   
        lobbyService.removePlayerFromLobby(id, playerId);

        webSocketService.sendMessage("/topic/lobby" + id + "/removePlayer", new RemovePlayerDTO(playerId));
    }

    @GetMapping("/{id}/role/{playerId}")
    @ResponseStatus(HttpStatus.OK)
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
    public void changePlayerRole(@PathVariable Long id, @PathVariable Long playerId, @RequestBody RoleUpdateDTO roleUpdate) {
        Player updatedPlayer = lobbyService.changePlayerRole(id, playerId, roleUpdate.getRole());
        if (updatedPlayer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role or player not found");
        }
        PlayerUpdateDTO playerUpdateDTO = DTOMapper.INSTANCE.convertEntityToPlayerUpdateDTO(updatedPlayer);
        playerUpdateDTO.setColor(updatedPlayer.getTeam() != null ? updatedPlayer.getTeam().getColor() : null);
        playerUpdateDTO.setRole(updatedPlayer.getRole() != null ? updatedPlayer.getRole().name() : null);
        webSocketService.sendMessage("/topic/lobby" + id + "/players", playerUpdateDTO);
    }

    @GetMapping("/{id}/team/{playerId}")
    @ResponseStatus(HttpStatus.OK)
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

        return new PlayerTeamDTO(player.getTeam().getColor());
    }

    @PutMapping("/{id}/team/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePlayerTeam(@PathVariable Long id, @PathVariable Long playerId, @RequestBody TeamUpdateDTO teamUpdate) {
        Player updatedPlayer = lobbyService.changePlayerTeam(id, playerId, teamUpdate.getColor());
        if (updatedPlayer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid team color or player not found");
        }
        PlayerUpdateDTO playerUpdateDTO = DTOMapper.INSTANCE.convertEntityToPlayerUpdateDTO(updatedPlayer);
        playerUpdateDTO.setColor(updatedPlayer.getTeam() != null ? updatedPlayer.getTeam().getColor() : null);
        playerUpdateDTO.setRole(updatedPlayer.getRole() != null ? updatedPlayer.getRole().name() : null);
        webSocketService.sendMessage("/topic/lobby" + id + "/players", playerUpdateDTO);
    }

    @GetMapping("/{id}/status/{playerId}")
    @ResponseStatus(HttpStatus.OK)
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
    public void setPlayerReadyStatus(@PathVariable Long id, @PathVariable Long playerId, @RequestBody ReadyStatusDTO statusUpdate) {
        Player updatedPlayer = lobbyService.setPlayerReadyStatus(id, playerId, statusUpdate.getReady(), webSocketService);
        if (updatedPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby or player not found");
        }
        PlayerUpdateDTO playerUpdateDTO = DTOMapper.INSTANCE.convertEntityToPlayerUpdateDTO(updatedPlayer);
        playerUpdateDTO.setColor(updatedPlayer.getTeam() != null ? updatedPlayer.getTeam().getColor() : null);
        playerUpdateDTO.setRole(updatedPlayer.getRole() != null ? updatedPlayer.getRole().name() : null);
        webSocketService.sendMessage("/topic/lobby" + id + "/players", playerUpdateDTO);
    }

    @ResponseStatus(HttpStatus.CREATED)
    public GetLobbyDTO getOrCreateLobby() {
        return DTOMapper.INSTANCE.convertEntitytoGetLobbyDTO(lobbyService.getOrCreateLobby());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateGameMode(@PathVariable Long id, @RequestBody GameMode gameMode) {
        var lobby = lobbyService.setGameMode(id, gameMode);
        webSocketService.sendMessage("/topic/lobby" + id + "/gameMode", DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby));
    }
}
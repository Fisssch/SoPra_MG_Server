package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/lobbies")
public class LobbyController {

    private final LobbyService lobbyService;

    @Autowired
    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
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
                lobby.getLobbyID(),
                lobby.getLobbyName(), // <-- dieser war bisher nicht drin
                lobby.getGameMode().name()
        );
    }

    @PutMapping("/{id}/{playerId}")
    @ResponseStatus(HttpStatus.OK)
    public PlayerResponseDTO addPlayerToLobby(@PathVariable Long id, @PathVariable Long playerId) {
        Player player = new Player();
        player.setId(playerId);
        Player addedPlayer = lobbyService.addPlayerToLobby(id, player);

        if (addedPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby or player not found");
        }

        PlayerResponseDTO response = new PlayerResponseDTO();
        response.setId(addedPlayer.getId());
        response.setRole(addedPlayer.getRole());
        response.setReady(addedPlayer.getReady());
        if (addedPlayer.getTeam() != null) {
            response.setTeamColor(addedPlayer.getTeam().getColor());
        }

        return response;
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

        return new PlayerRoleDTO(player.getRole());
    }

    @PutMapping("/{id}/role/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePlayerRole(@PathVariable Long id, @PathVariable Long playerId, @RequestBody RoleUpdateDTO roleUpdate) {
        boolean changed = lobbyService.changePlayerRole(id, playerId, roleUpdate.getRole());
        if (!changed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role or player not found");
        }
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
        boolean changed = lobbyService.changePlayerTeam(id, playerId, teamUpdate.getColor());
        if (!changed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid team color or player not found");
        }
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
        boolean updated = lobbyService.setPlayerReadyStatus(id, playerId, statusUpdate.getReady());
        if (!updated) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby or player not found");
        }
    }
}
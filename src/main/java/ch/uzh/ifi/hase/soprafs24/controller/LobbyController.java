package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.Team;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/lobbies")
public class LobbyController {
    private final LobbyService lobbyService;

    @Autowired
    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @PostMapping
    public ResponseEntity<LobbyResponseDTO> createLobby(@RequestBody LobbyPostDTO lobbyPostDTO) {
        GameMode mode;
        try {
            mode = GameMode.valueOf(lobbyPostDTO.getGameMode().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body(null); // oder mit ErrorDTO
        }

        Lobby lobby = lobbyService.createLobby(lobbyPostDTO.getLobbyName(), mode); // <--- richtiger Call!

        LobbyResponseDTO response = new LobbyResponseDTO(lobby.getLobbyID());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PutMapping("/{id}/{playerId}")
    public ResponseEntity<PlayerResponseDTO> addPlayerToLobby(@PathVariable Long id, @PathVariable Long playerId) {
        Player player = new Player();
        player.setId(playerId);
        Player addedPlayer = lobbyService.addPlayerToLobby(id, player);

        if (addedPlayer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        PlayerResponseDTO response = new PlayerResponseDTO();
        response.setId(addedPlayer.getId());
        response.setRole(addedPlayer.getRole());
        response.setReady(addedPlayer.getReady());
        if (addedPlayer.getTeam() != null) {
            response.setTeamColor(addedPlayer.getTeam().getColor());
        }

        return ResponseEntity.ok(response);
    }
    @GetMapping("/{id}/role/{playerId}")
    public ResponseEntity<PlayerRoleDTO> getPlayerRole(@PathVariable Long id, @PathVariable Long playerId) {
        Lobby lobby = lobbyService.getLobbyById(id);
        if (lobby == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        Player player = lobby.getPlayers().stream().filter(p -> playerId.equals(p.getId())).findFirst().orElse(null);
        if (player == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        PlayerRoleDTO dto = new PlayerRoleDTO(player.getRole());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}/role/{playerId}")
    public ResponseEntity<?> changePlayerRole(@PathVariable Long id, @PathVariable Long playerId, @RequestBody RoleUpdateDTO roleUpdate) {
        boolean changed = lobbyService.changePlayerRole(id, playerId, roleUpdate.getRole());
        return changed
                ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDTO("Invalid role or player not found"));
    }

    @GetMapping("/{id}/team/{playerId}")
    public ResponseEntity<PlayerTeamDTO> getPlayerTeam(@PathVariable Long id, @PathVariable Long playerId) {
        Lobby lobby = lobbyService.getLobbyById(id);
        if (lobby == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Player player = lobby.getPlayers().stream()
                .filter(p -> playerId.equals(p.getId()))
                .findFirst()
                .orElse(null);

        if (player == null || player.getTeam() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        PlayerTeamDTO dto = new PlayerTeamDTO(player.getTeam().getColor());
        return ResponseEntity.ok(dto);
    }
    @PutMapping("/{id}/team/{playerId}")
    public ResponseEntity<?> changePlayerTeam(@PathVariable Long id, @PathVariable Long playerId, @RequestBody TeamUpdateDTO teamUpdate) {
        boolean changed = lobbyService.changePlayerTeam(id, playerId, teamUpdate.getColor());
        return changed
                ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDTO("Invalid team color or player not found"));
    }

    @GetMapping("/{id}/status/{playerId}")
    public ResponseEntity<ReadyStatusDTO> getPlayerReadyStatus(@PathVariable Long id, @PathVariable Long playerId) {
        Boolean ready = lobbyService.getPlayerReadyStatus(id, playerId);
        if (ready == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        ReadyStatusDTO dto = new ReadyStatusDTO();
        dto.setReady(ready);
        return ResponseEntity.ok(dto);
    }
    @PutMapping("/{id}/status/{playerId}")
    public ResponseEntity<?> setPlayerReadyStatus(@PathVariable Long id, @PathVariable Long playerId, @RequestBody ReadyStatusDTO statusUpdate) {
        boolean updated = lobbyService.setPlayerReadyStatus(id, playerId, statusUpdate.getReady());
        return updated
                ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDTO("Lobby or player not found"));
    }
}

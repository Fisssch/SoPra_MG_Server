package ch.uzh.ifi.hase.soprafs24.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.Team;
import ch.uzh.ifi.hase.soprafs24.repository.*;

@Service
@Transactional
public class LobbyService {

    private final Logger log = LoggerFactory.getLogger(LobbyService.class);
    private final LobbyRepository lobbyRepository;
    private final PlayerRepository playerRepository;

    public LobbyService(LobbyRepository lobbyRepository, PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
        this.lobbyRepository = lobbyRepository;
    }

    public Lobby getLobbyById(Long id) {
        return lobbyRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found with id: " + id));
    }

    public Lobby getOrCreateLobby() {
        Lobby lobby = lobbyRepository.findAll().stream().findFirst().orElse(null); // TODO: Change to findByLobbyCode if needed
        if (lobby == null) {
            return createLobby("Active lobby", GameMode.CLASSIC);
        }
        return lobby;
    }

    public Lobby setGameMode(Long id, GameMode gameMode) {
        Lobby lobby = getLobbyById(id);
        lobby.setGameMode(gameMode);
        lobbyRepository.save(lobby);
        return lobby;
    }

    public Player addPlayerToLobby(Long lobbyId, Long playerId) {
        Player player = playerRepository.findById(playerId).orElse(new Player(playerId));
        Lobby lobby = getLobbyById(lobbyId);
        if (lobby == null) return null;

        long redCount = lobby.getPlayers().stream()
                .filter(p -> "red".equals(p.getTeam().getColor()))
                .count();

        long blueCount = lobby.getPlayers().stream()
                .filter(p -> "blue".equals(p.getTeam().getColor()))
                .count();

        Team assignedTeam = redCount <= blueCount ? lobby.getRedTeam() : lobby.getBlueTeam();
        player.setTeam(assignedTeam);

        if (assignedTeam.getSpymaster() == null) {
            player.setRole(PlayerRole.SPYMASTER);
            assignedTeam.setSpymaster(player);
        } else {
            player.setRole(PlayerRole.FIELD_OPERATIVE);
        }

        lobby.addPlayer(player);
        lobbyRepository.save(lobby);
        playerRepository.save(player);
        return player;
    }

    public void removePlayerFromLobby(Long lobbyId, Long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found with id: " + playerId));
        Lobby lobby = getLobbyById(lobbyId);

        lobby.removePlayer(player);
        playerRepository.delete(player);
        lobbyRepository.save(lobby);
    }

    public Lobby createLobby(String lobbyName, GameMode gameMode) {
        Lobby lobby = new Lobby();
        lobby.setLobbyName(lobbyName);
        lobby.setGameMode(gameMode);
        lobby.setLobbyCode(generateLobbyCode());

        Team redTeam = new Team();
        redTeam.setColor("red");
        Team blueTeam = new Team();
        blueTeam.setColor("blue");

        lobby.setRedTeam(redTeam);
        lobby.setBlueTeam(blueTeam);

        return lobbyRepository.save(lobby);
    }

    private int generateLobbyCode() {
        return (int) (Math.random() * 9000) + 1000;
    }

    public Player changePlayerTeam(Long lobbyId, Long playerId, String color) {
        Lobby lobby = getLobbyById(lobbyId);

        Player player = playerRepository.findById(playerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found with id: " + playerId));

        if ("red".equalsIgnoreCase(color)) {
            player.setTeam(lobby.getRedTeam());
        } else if ("blue".equalsIgnoreCase(color)) {
            player.setTeam(lobby.getBlueTeam());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid team color: " + color);
        }

        playerRepository.save(player);
        return player;
    }

    public Player changePlayerRole(Long lobbyId, Long playerId, String roleStr) {
        Player player = playerRepository.findById(playerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found with id: " + playerId));

        PlayerRole role;
        try {
            role = PlayerRole.valueOf(roleStr.toUpperCase().replace(" ", "_"));
            player.setRole(role);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleStr);
        }

        playerRepository.save(player);
        return player;
    }

    public Boolean getPlayerReadyStatus(Long lobbyId, Long playerId) {
        Lobby lobby = getLobbyById(lobbyId);
        if (lobby == null) return null;

        Player player = playerRepository.findById(playerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found with id: " + playerId));

        return player.getReady();
    }

    public Player setPlayerReadyStatus(Long lobbyId, Long playerId, boolean ready, WebsocketService websocketService) {
        Lobby lobby = getLobbyById(lobbyId);

        Player player = playerRepository.findById(playerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found with id: " + playerId));

        player.setReady(ready);

        if (shouldStartGame(lobby)) {
            lobby.setGameStarted(true);
            websocketService.sendMessage("/topic/lobby/" + lobbyId + "/start", true);
        }

        playerRepository.save(player);
        return player;
    }

    public boolean shouldStartGame(Lobby lobby) {
        return lobby.getPlayers().size() >= 4 &&
                lobby.getPlayers().stream().allMatch(p -> Boolean.TRUE.equals(p.getReady()));
    }
}

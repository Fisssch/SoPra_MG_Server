package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
public class LobbyService {

    private final Logger log = LoggerFactory.getLogger(LobbyService.class);
    private final LobbyRepository lobbyRepository;

    public LobbyService(LobbyRepository lobbyRepository) {
        this.lobbyRepository = lobbyRepository;
    }

    public Lobby getLobbyById(Long id) {
        return lobbyRepository.findById(String.valueOf(id)).orElse(null);
    }

    public Lobby getOrCreateLobby() {
        return new Lobby(); // TODO: ggf. mit Session arbeiten
    }

    public Lobby setGameMode(Integer id, GameMode gameMode) {
        Lobby lobby = new Lobby(); // TODO: tatsächliche Lobby per ID holen
        lobby.setGameMode(gameMode);
        // TODO: Lobby speichern, evtl. WebSocket-Benachrichtigung senden
        return lobby;
    }

    public Player addPlayerToLobby(Long lobbyId, Player player) {
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
            player.setRole(PlayerRole.valueOf("spymaster"));
            assignedTeam.setSpymaster(player);
        } else {
            player.setRole(PlayerRole.valueOf("field operative"));
        }

        lobby.addPlayer(player);
        lobbyRepository.save(lobby);
        return player;
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

    public void saveLobby(Lobby lobby) {
        lobbyRepository.save(lobby);
    }

    public boolean changePlayerTeam(Long lobbyId, Long playerId, String color) {
        Lobby lobby = getLobbyById(lobbyId);
        if (lobby == null) return false;

        Player player = lobby.getPlayers().stream()
                .filter(p -> playerId.equals(p.getId()))
                .findFirst().orElse(null);

        if (player == null) return false;

        if ("red".equalsIgnoreCase(color)) {
            player.setTeam(lobby.getRedTeam());
        } else if ("blue".equalsIgnoreCase(color)) {
            player.setTeam(lobby.getBlueTeam());
        } else {
            return false;
        }

        lobbyRepository.save(lobby);
        return true;
    }

    public boolean changePlayerRole(Long lobbyId, Long playerId, String role) {
        Lobby lobby = getLobbyById(lobbyId);
        if (lobby == null) return false;

        Player player = lobby.getPlayers().stream()
                .filter(p -> playerId.equals(p.getId()))
                .findFirst().orElse(null);

        if (player == null) return false;

        if ("spymaster".equalsIgnoreCase(role) || "field operative".equalsIgnoreCase(role)) {
            player.setRole(PlayerRole.valueOf(role));
        } else {
            return false;
        }

        lobbyRepository.save(lobby);
        return true;
    }

    public Boolean getPlayerReadyStatus(Long lobbyId, Long playerId) {
        Lobby lobby = getLobbyById(lobbyId);
        if (lobby == null) return null;

        Player player = lobby.getPlayers().stream()
                .filter(p -> playerId.equals(p.getId()))
                .findFirst().orElse(null);

        return player != null ? player.getReady() : null;
    }

    public boolean setPlayerReadyStatus(Long lobbyId, Long playerId, boolean ready) {
        Lobby lobby = getLobbyById(lobbyId);
        if (lobby == null) return false;

        Player player = lobby.getPlayers().stream()
                .filter(p -> playerId.equals(p.getId()))
                .findFirst().orElse(null);

        if (player == null) return false;

        player.setReady(ready);

        if (shouldStartGame(lobby)) {
            lobby.setGameStarted(true);
            // TODO: WebSocket Nachricht an Clients senden
        }

        lobbyRepository.save(lobby);
        return true;
    }

    public boolean shouldStartGame(Lobby lobby) {
        return lobby.getPlayers().size() >= 4 &&
                lobby.getPlayers().stream().allMatch(p -> Boolean.TRUE.equals(p.getReady()));
    }
}
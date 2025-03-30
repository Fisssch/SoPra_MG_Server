package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import java.util.Comparator;

@Service
public class LobbyService {

    private final LobbyRepository lobbyRepository;

    @Autowired
    public LobbyService(LobbyRepository lobbyRepository) {
        this.lobbyRepository = lobbyRepository;
    }

    public Lobby getLobbyById(Long id) {
        return lobbyRepository.findById(id).orElse(null);
    }

    public Player addPlayerToLobby(Long lobbyId, Player player) {
        Lobby lobby = getLobbyById(lobbyId);

        long redCount = lobby.getPlayers().stream()
                .filter(p -> "red".equals(p.getTeam().getColor()))
                .count();

        long blueCount = lobby.getPlayers().stream()
                .filter(p -> "blue".equals(p.getTeam().getColor()))
                .count();

        Team assignedTeam = redCount <= blueCount ? lobby.getRedTeam() : lobby.getBlueTeam();
        player.setTeam(assignedTeam);

        if (assignedTeam.getSpymaster() == null) {
            player.setRole("spymaster");
            assignedTeam.setSpymaster(player);
        } else {
            player.setRole("field operative");
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
        return (int)(Math.random() * 9000) + 1000;
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
                .findFirst()
                .orElse(null);

        if (player == null) return false;

        if ("spymaster".equalsIgnoreCase(role)) {
            // Prüfe ob im selben Team bereits ein Spymaster existiert
            boolean teamAlreadyHasSpymaster = lobby.getPlayers().stream()
                    .filter(p -> p.getTeam() != null && player.getTeam() != null)
                    .filter(p -> player.getTeam().equals(p.getTeam()))
                    .anyMatch(p -> PlayerRole.SPYMASTER.equals(p.getRole()) && !p.getId().equals(player.getId()));

            if (teamAlreadyHasSpymaster) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This team already has a Spymaster.");
            }

            player.setRole(PlayerRole.SPYMASTER);
        }
        else if ("field operative".equalsIgnoreCase(role)) {
            player.setRole(PlayerRole.FIELD_OPERATIVE);
        }
        else {
            return false; // ungültige Rolle
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
        lobbyRepository.save(lobby);
        return true;
    }
}

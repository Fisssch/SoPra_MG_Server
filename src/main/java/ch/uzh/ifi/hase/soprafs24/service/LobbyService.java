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

        // Team-Zuweisung: balance nach Teamgröße
        long redCount = lobby.getPlayers().stream()
                .filter(p -> "red".equals(p.getTeam().getColor()))
                .count();

        long blueCount = lobby.getPlayers().stream()
                .filter(p -> "blue".equals(p.getTeam().getColor()))
                .count();

        Team assignedTeam = redCount <= blueCount ? lobby.getRedTeam() : lobby.getBlueTeam();
        player.setTeam(assignedTeam);

        // Rolle: falls kein Spymaster, dann Spymaster – sonst Field Operative
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

        // Red Team erstellen
        Team redTeam = new Team();
        redTeam.setColor("red");

        // Blue Team erstellen
        Team blueTeam = new Team();
        blueTeam.setColor("blue");

        // Teams setzen
        lobby.setRedTeam(redTeam);
        lobby.setBlueTeam(blueTeam);

        return lobbyRepository.save(lobby);
    }
    private int generateLobbyCode() {
        return (int)(Math.random() * 9000) + 1000; // 4-stelliger zufälliger Code
    }
}
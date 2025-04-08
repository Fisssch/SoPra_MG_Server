package ch.uzh.ifi.hase.soprafs24.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
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
    private final TeamRepository teamRepository;

    public LobbyService(LobbyRepository lobbyRepository, PlayerRepository playerRepository, TeamRepository teamRepository) {
        this.playerRepository = playerRepository;
        this.lobbyRepository = lobbyRepository;
        this.teamRepository = teamRepository;
    }

    public Lobby getLobbyById(Long id) {
        return lobbyRepository.findById(id).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found with id: " + id));
    }
    public Lobby getOrCreateLobby() {
        return getOrCreateLobby(null);
    }
    public Lobby getOrCreateLobby(Integer lobbyCode) {
        Lobby lobby = null;

        if (lobbyCode != null) {
            lobby = lobbyRepository.findByLobbyCode(lobbyCode)
                    .orElse(null); // nicht orElseThrow – du willst ggf. eine neue Lobby erstellen
        }

        if (lobby == null) {
            return createLobby("Lobby " + generateLobbyCode(), GameMode.CLASSIC);
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
                .filter(p -> TeamColor.RED.equals(p.getTeam().getColor()))
                .count();

        long blueCount = lobby.getPlayers().stream()
                .filter(p -> TeamColor.BLUE.equals(p.getTeam().getColor()))
                .count();

        Team assignedTeam = redCount <= blueCount ? lobby.getRedTeam() : lobby.getBlueTeam();
        player.setTeam(assignedTeam);

        if (assignedTeam.getSpymaster() == null) {
            player.setRole(PlayerRole.SPYMASTER);
            assignedTeam.setSpymaster(player);
        } else {
            player.setRole(PlayerRole.FIELD_OPERATIVE);
        }
        
        playerRepository.save(player);
        lobby.addPlayer(player);
        lobbyRepository.save(lobby);
        return player;
    }

    public void removePlayerFromLobby(Long lobbyId, Long playerId) {
        Lobby lobby = getLobbyById(lobbyId);
        if (lobby == null) return;

        Player player = lobby.getPlayers().stream()
                .filter(p -> playerId.equals(p.getId()))
                .findFirst()
                .orElse(null);

        if (player != null) {
            lobby.removePlayer(player);
            playerRepository.delete(player); // remove all players
        }

        if (lobby.getPlayers().isEmpty()) {
            lobbyRepository.delete(lobby); // remove Lobby
        } else {
            lobbyRepository.save(lobby);
        }
    }

    public Lobby createLobby(String lobbyName, GameMode gameMode) {
        Lobby lobby = new Lobby();
        lobby.setLobbyName(lobbyName);
        lobby.setGameMode(gameMode);
        lobby.setLobbyCode(generateLobbyCode());

        // Save the lobby first to get an ID
        lobby = lobbyRepository.save(lobby);

        Team redTeam = new Team();
        redTeam.setColor(TeamColor.RED);
        redTeam.setLobby(lobby);
        teamRepository.save(redTeam);
        
        Team blueTeam = new Team();
        blueTeam.setColor(TeamColor.BLUE);
        blueTeam.setLobby(lobby);
        teamRepository.save(blueTeam);

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
            lobby.assignPlayerToTeam(player, lobby.getRedTeam());
        } else if ("blue".equalsIgnoreCase(color)) {
            lobby.assignPlayerToTeam(player, lobby.getBlueTeam());
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
    public Lobby getLobbyByCode(Integer code) {
        return lobbyRepository.findByLobbyCode(code).orElse(null);

    public Lobby addCustomWord(Long lobbyId, String word){
        Lobby lobby = lobbyRepository.findById(lobbyId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        if (word == null || word.trim().isEmpty() || word.contains(" ")){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid word"); 
        }

        if (lobby.getCustomWords().size() >= 25){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum of 25 custom words reached");
        }

        lobby.addCustomWord(word);
        return lobbyRepository.save(lobby);
    }
}

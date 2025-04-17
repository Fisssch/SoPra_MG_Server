package ch.uzh.ifi.hase.soprafs24.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.Team;
import ch.uzh.ifi.hase.soprafs24.repository.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyPlayerStatusDTO;


@Service
@Transactional
public class LobbyService {

    private final Logger log = LoggerFactory.getLogger(LobbyService.class);
    private final LobbyRepository lobbyRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final WebsocketService websocketService;

    public LobbyService(LobbyRepository lobbyRepository, PlayerRepository playerRepository, TeamRepository teamRepository, WebsocketService websocketService) {
        this.playerRepository = playerRepository;
        this.lobbyRepository = lobbyRepository;
        this.teamRepository = teamRepository;
        this.websocketService = websocketService;
    }
    
    public Lobby getOrCreateLobby(Integer lobbyCode) {
        Lobby lobby = null;

        if (lobbyCode != null) {
            lobby = lobbyRepository.findByLobbyCode(lobbyCode)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby with code " + lobbyCode + " not found"));
        }

        if (lobby == null) {
            return createLobby("Lobby " + generateLobbyCode(), GameMode.CLASSIC);
        }

        return lobby;
    }

    public Lobby getLobbyById(Long id) {
        return lobbyRepository.findById(id).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found with id: " + id));
    }

    public Lobby createLobby(String lobbyName, GameMode gameMode) {
        Lobby lobby = new Lobby();
        lobby.setLobbyName(lobbyName);
        lobby.setGameMode(gameMode);
        lobby.setLobbyCode(generateLobbyCode());

        lobby.setCreatedAt(Instant.now());
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

        lobby.setTheme("default");

        scheduleLobbyTimeout(lobby);

        return lobbyRepository.save(lobby);
    }

    public Lobby setGameMode(Long id, GameMode gameMode) {
        Lobby lobby = getLobbyById(id);
        lobby.setGameMode(gameMode);

        if (gameMode != GameMode.THEME){
            lobby.setTheme(null); 
        }
        lobbyRepository.save(lobby);
        return lobby;
    }

    public Lobby setTheme(Long id, String theme){
        Lobby lobby = getLobbyById(id); 
        lobby.setTheme(theme.trim());
        return lobbyRepository.save(lobby);
    }

    public Player addPlayerToLobby(Long lobbyId, Long playerId) {
        Player player = playerRepository.findById(playerId).orElse(new Player(playerId));
        Lobby lobby = getLobbyById(lobbyId);
        if (lobby == null) return null;

        // Prüfen, ob Spieler bereits in Lobby
        if (lobby.getPlayers().stream().anyMatch(p -> p.getId().equals(playerId))) {
            return player; // Spieler ist schon drin, nichts ändern
        }

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

        player.setReady(false);
        playerRepository.save(player);
        lobby.addPlayer(player);
        teamRepository.save(assignedTeam);
        lobbyRepository.save(lobby);

        sendLobbyPlayerStatusUpdate(lobbyId);

        return player;
    }

    public void removePlayerFromLobby(Long lobbyId, Long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found with id: " + playerId));
        Lobby lobby = getLobbyById(lobbyId);

        if (player.getRole() == PlayerRole.SPYMASTER) {
            Team team = player.getTeam();
            if (team != null) {
                team.setSpymaster(null);
                teamRepository.save(team);
            }
        }

        lobby.removePlayer(player);
        lobbyRepository.save(lobby);
        playerRepository.delete(player);

        Lobby updatedLobby = getLobbyById(lobbyId);

        if (updatedLobby.getPlayers().isEmpty()) {
            if (updatedLobby.getRedTeam() != null) {
                teamRepository.delete(updatedLobby.getRedTeam());
            }
            if (updatedLobby.getBlueTeam() != null) {
                teamRepository.delete(updatedLobby.getBlueTeam());
            }
            lobbyRepository.delete(updatedLobby);
        } else {
            sendLobbyPlayerStatusUpdate(lobbyId); // WebSocket-Update
        }
    }

    public Player changePlayerTeam(Long lobbyId, Long playerId, String color) {
        Lobby lobby = getLobbyById(lobbyId);

        Player player = playerRepository.findById(playerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found with id: " + playerId));

        if (player.getRole() == PlayerRole.SPYMASTER) {
            Team team = player.getTeam();
            if (team != null) {
                team.setSpymaster(null);
                teamRepository.save(team);
            }
        }
        Team newTeam;
        if ("red".equalsIgnoreCase(color)) {
            newTeam = lobby.getRedTeam();
        } else if ("blue".equalsIgnoreCase(color)) {
            newTeam = lobby.getBlueTeam();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid team color: " + color);
        }
        lobby.assignPlayerToTeam(player, newTeam);
        // If the player is a spymaster but the other team already has a spymaster set him to field operative, otherwise assign him to spymaster of new team
        if (player.getRole() == PlayerRole.SPYMASTER) {
            if (newTeam.getSpymaster() == null) {
                newTeam.setSpymaster(player);
                teamRepository.save(newTeam);
            }
            else
                player.setRole(PlayerRole.FIELD_OPERATIVE);
        }

        playerRepository.save(player);
        lobbyRepository.save(lobby);
        return player;
    }

    public Player changePlayerRole(Long lobbyId, Long playerId, String roleStr) {
        Player player = playerRepository.findById(playerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found with id: " + playerId));

        PlayerRole oldRole = player.getRole();
        PlayerRole role;
        try {
            role = PlayerRole.valueOf(roleStr.toUpperCase().replace(" ", "_"));
            player.setRole(role);
            if (role == PlayerRole.SPYMASTER) {
                // If the player is set to spymaster, set the spymaster of the team to this player
                Team team = player.getTeam();
                if (team != null) {
                    if (team.getSpymaster() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Team already has a spymaster");
                    team.setSpymaster(player);
                    teamRepository.save(team);
                }
            } 
            if (oldRole == PlayerRole.SPYMASTER) {
                // If the player was a spymaster, set the spymaster of the team to null
                Team team = player.getTeam();
                if (team != null && team.getSpymaster() != null && team.getSpymaster().getId().equals(player.getId())) { //important to work with id here otherwise the if statement fails 
                    team.setSpymaster(null);
                    teamRepository.save(team);
                }
            }
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
        playerRepository.save(player);

        sendLobbyPlayerStatusUpdate(lobbyId);

        if (shouldStartGame(lobby)) {
            lobby.setGameStarted(true);
            lobbyRepository.save(lobby); 
            websocketService.sendMessage("/topic/lobby/" + lobbyId + "/start", true);
        }
        return player;
    }

    public Lobby addCustomWord(Long lobbyId, String word){
        Lobby lobby = lobbyRepository.findById(lobbyId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        if (lobby.getGameMode() != GameMode.OWN_WORDS){
            throw new ResponseStatusException((HttpStatus.BAD_REQUEST), "Can't add custom words unless game mode = OWN_WORDS"); 
        }

        if (word == null || word.trim().isEmpty() || word.contains(" ")){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid word"); 
        }

        if (lobby.getCustomWords().size() >= 25){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum of 25 custom words reached");
        }

        lobby.addCustomWord(word);
        return lobbyRepository.save(lobby);
    }

    public Lobby removeCustomWord(Long id, String word) {
        Lobby lobby = getLobbyById(id);
        
        if (lobby.getGameMode() != GameMode.OWN_WORDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove words unless in OWN_WORDS mode");
        }
    
        if (!lobby.getCustomWords().removeIf(w -> w.equalsIgnoreCase(word))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found");
        }
    
        return lobbyRepository.save(lobby);
    }

    public boolean shouldStartGame(Lobby lobby) {
        if (!lobby.getPlayers().stream().allMatch(p -> Boolean.TRUE.equals(p.getReady()))) {
            return false;
        }
        if (lobby.getPlayers().size() < 4) {
            websocketService.sendMessage("/topic/lobby/" + lobby.getId() + "/readyError", "Not enough players in the lobby to start the game.");
            return false;
        }
        if (lobby.getRedTeam().getSpymaster() == null) {
            websocketService.sendMessage("/topic/lobby/" + lobby.getId() + "/readyError", "Red team needs a spymaster to start the game.");
            return false;
        }
        if (lobby.getBlueTeam().getSpymaster() == null) {
            websocketService.sendMessage("/topic/lobby/" + lobby.getId() + "/readyError", "Blue team needs a spymaster to start the game.");
            return false;
        }
        if (lobby.getRedTeam().getPlayers().size() < 2 || lobby.getBlueTeam().getPlayers().size() < 2) {
            websocketService.sendMessage("/topic/lobby/" + lobby.getId() + "/readyError", "Both teams need at least 2 players to start the game.");
            return false;
        }
        return true;
    }

    private int generateLobbyCode() {
        return (int) (Math.random() * 9000) + 1000;
    }

    private void sendLobbyPlayerStatusUpdate(Long lobbyId) {
        Lobby lobby = getLobbyById(lobbyId);
        int total = lobby.getPlayers().size();
        int ready = (int) lobby.getPlayers().stream().filter(p -> Boolean.TRUE.equals(p.getReady())).count();

        websocketService.sendMessage("/topic/lobby/" + lobbyId + "/playerStatus",
                new LobbyPlayerStatusDTO(total, ready));
    }

    public void scheduleLobbyTimeout(Lobby lobby) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Lobby currentLobby = getLobbyById(lobby.getId());
                if (!currentLobby.isGameStarted()) {
                    closeLobby(currentLobby.getId());
                }
            }
        }, 10*60 * 1000); // 10 Minuten in Millisekunden
    }
    
    public void closeLobby(Long lobbyId) {
        Lobby lobby;
        try {
            lobby = getLobbyById(lobbyId);
        }
        catch (Exception ex)
        { return; } // Lobby already closed

        // Notify all players via WebSocket
        websocketService.sendMessage("/topic/lobby/" + lobbyId + "/close", "CLOSED");

        // Remove all players
        for (Player player : lobby.getPlayers()) {
            playerRepository.delete(player);
        }

        // Delete teams
        if (lobby.getRedTeam() != null) {
            teamRepository.delete(lobby.getRedTeam());
        }
        if (lobby.getBlueTeam() != null) {
            teamRepository.delete(lobby.getBlueTeam());
        }

        // Delete the lobby itself
        lobbyRepository.delete(lobby);

        log.info("Lobby " + lobbyId + " has been closed due to inactivity.");
    }
}

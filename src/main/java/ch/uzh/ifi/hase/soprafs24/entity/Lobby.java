package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "LOBBY")
public class Lobby extends DatabaseEntity {

    private static final long serialVersionUID = 1L;

    private String lobbyName;

    private GameMode gameMode;

    @DBRef(lazy = true)
    private List<Player> players = new ArrayList<>();

    @Indexed(unique = true)
    private Integer lobbyCode;
    
    @DBRef(lazy = true)
    private Team redTeam;

    @DBRef(lazy = true)
    private Team blueTeam;
    
    private boolean gameStarted = false;
    
    // --- Getter & Setter ---

    public Long getLobbyID() {
        return getId();
    }

    public void setLobbyID(Long lobbyID) {
        setId(lobbyID);
    }

    public String getLobbyName() {
        return lobbyName;
    }

    public void setLobbyName(String lobbyName) {
        this.lobbyName = lobbyName;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        if (gameMode != null)
            this.gameMode = gameMode;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public void addPlayer(Player player) {
        if (this.players == null) {
            this.players = new ArrayList<>();
        }
        this.players.add(player);
    }

    public void removePlayer(Player player) {
        if (this.players == null) {
            this.players = new ArrayList<>();
        }
        if (this.players.contains(player)) {
            this.players.remove(player);
        }
    }

    public Integer getLobbyCode() {
        return lobbyCode;
    }

    public void setLobbyCode(Integer lobbyCode) {
        this.lobbyCode = lobbyCode;
    }

    public Team getRedTeam() {
        return redTeam;
    }

    public void setRedTeam(Team redTeam) {
        this.redTeam = redTeam;
    }

    public Team getBlueTeam() {
        return blueTeam;
    }

    public void setBlueTeam(Team blueTeam) {
        this.blueTeam = blueTeam;
    }

    public List<Player> getPlayersByTeam(Team team) {
        if (players == null || team == null) {
            return new ArrayList<>();
        }
        
        return players.stream()
                .filter(p -> p.getTeam() != null && 
                        (p.getTeam().getId().equals(team.getId()) || 
                         (p.getTeam().getColor() != null && team.getColor() != null && 
                          p.getTeam().getColor().equals(team.getColor()))))
                .collect(Collectors.toList());
    }
    
    /**
     * Assigns a player to a specific team
     * ATTENTION: this method does not save the player to the database, so playerRepository.save(player) must be called afterwards!
     * @param player The player to assign
     * @param team The team to assign the player to
     */
    public void assignPlayerToTeam(Player player, Team team) {
        if (player == null || team == null) {
            return;
        }
        
        // Add player to lobby if not already present
        if (!players.contains(player)) {
            this.addPlayer(player);
        }
        
        // Set the player's team reference
        player.setTeam(team);
    }
    
    /**
     * Removes a player from a specific team
     * ATTENTION: this method does not save the player to the database, so playerRepository.save(player) must be called afterwards!
     * @param player The player to remove
     * @param team The team the player belongs to
     */
    public void removePlayerFromTeam(Player player, Team team) {
        if (player == null || team == null) {
            return;
        }
        
        // Only remove the team reference if the player belongs to this team
        if (player.getTeam() != null && 
            (player.getTeam().getId().equals(team.getId()) || 
             (player.getTeam().getColor() != null && team.getColor() != null && 
              player.getTeam().getColor().equals(team.getColor())))) {
            player.setTeam(null);
        }
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }
}
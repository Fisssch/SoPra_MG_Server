package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;

import java.time.Instant;
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

    private List<String> customWords = new ArrayList<>();

    private Instant createdAt;
    
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    public void setGameMode(GameMode gameMode) {
        if (gameMode != null)
            this.gameMode = gameMode;
    }

    public List<Player> getPlayers() {
        return players == null ? new ArrayList<>() : players;
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
        if (this.players == null || player == null) {
            return;
        }
        this.players.removeIf(p -> p.getId().equals(player.getId()));
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
        redTeam.setLobby(this);
    }

    public Team getBlueTeam() {
        return blueTeam;
    }

    public void setBlueTeam(Team blueTeam) {
        this.blueTeam = blueTeam;
        blueTeam.setLobby(this);
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

    public List<String> getCustomWords(){
        return customWords;
    }

    public void setCustomWords(List<String> customWords){
        this.customWords = customWords; 
    }

    public void addCustomWord(String word){
        if (this.customWords == null){
            this.customWords = new ArrayList<>();
        }
        if (!this.customWords.contains(word.toUpperCase())){
            this.customWords.add(word.toUpperCase());
        }
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

        if (players == null) {
            players = new ArrayList<>();
        }

        boolean alreadyInLobby = players.stream().anyMatch(p->p.getId().equals(player.getId())); 
        if (!alreadyInLobby){
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
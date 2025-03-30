package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "LOBBY")
public class Lobby {

    @Id
    private String id;

    private String lobbyName;
    private GameMode gameMode;
    private List<Player> players = new ArrayList<>();
    private Integer lobbyCode;
    private Team redTeam;
    private Team blueTeam;
    private boolean gameStarted = false;

    // --- Getter & Setter ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
        this.gameMode = gameMode;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public void addPlayer(Player player) {
        this.players.add(player);
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

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }
}
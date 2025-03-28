package ch.uzh.ifi.hase.soprafs24.entity;
import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "LOBBY")
public class Lobby extends DatabaseEntity {

    private static final long serialVersionUID = 1L;

    private String lobbyName;

    private GameMode gameMode;

    private List<Player> players = new ArrayList<>(); // TODO: Teste öb das funktioniert

    @Indexed(unique = true)
    private Integer lobbyCode;
  
    private Team redTeam;

    private Team blueTeam;

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
}

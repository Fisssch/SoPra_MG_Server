package ch.uzh.ifi.hase.soprafs24.entity;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;

import java.util.List;
import java.util.ArrayList;

@Document(collection =  "TEAM")
public class Team extends DatabaseEntity {

    private static final long serialVersionUID = 1L;

    private TeamColor color;
    
    @DBRef(lazy = true)
    private Lobby lobby;
    
    @DBRef(lazy = true)
    private Player spymaster;

    public TeamColor getColor() {
        return color;
    }

    public void setColor(TeamColor color) {
        this.color = color;
    }

    public List<Player> getPlayers() {
        if (lobby != null) {
            return lobby.getPlayersByTeam(this);
        }
        return new ArrayList<>();
    }

    public void addPlayer(Player player) {
        if (lobby != null && player != null) {
            lobby.assignPlayerToTeam(player, this);
        }
    }

    public void setPlayers(List<Player> players) {
        if (lobby != null && players != null) {
            for (Player player : players) {
                addPlayer(player);
            }
        }
    }

    public void removePlayer(Player player) {
        if (lobby != null && player != null) {
            lobby.removePlayerFromTeam(player, this);
        }
    }

    public Lobby getLobby() {
        return lobby;
    }

    public void setLobby(Lobby lobby) {
        this.lobby = lobby;
    }

    public Player getSpymaster() {
        return spymaster;
    }

    public void setSpymaster(Player spymaster) {
        this.spymaster = spymaster;
    }
}
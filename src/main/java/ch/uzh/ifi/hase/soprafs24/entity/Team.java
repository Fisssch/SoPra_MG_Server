package ch.uzh.ifi.hase.soprafs24.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;

@Document(collection =  "TEAM")
public class Team extends DatabaseEntity {

    private static final long serialVersionUID = 1L;

    private String color; // "red" oder "blue"

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    private List<Player> players;

    @OneToOne
    private Player spymaster;

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public Player getSpymaster() {
        return spymaster;
    }

    public void setSpymaster(Player spymaster) {
        this.spymaster = spymaster;
    }

    // Optional: assignSpymaster() Logik folgt später
}
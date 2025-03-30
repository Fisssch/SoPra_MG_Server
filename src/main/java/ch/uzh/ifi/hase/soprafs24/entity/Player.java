
package ch.uzh.ifi.hase.soprafs24.entity;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;

@Document(collection = "PLAYER")
public class Player extends DatabaseEntity {

    private static final long serialVersionUID = 1L;

    @DBRef(lazy = true)
    private Team team;

    @DBRef(lazy = true)
    private PlayerRole role; // "spymaster" oder "field operative"

    private Boolean ready;

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
    public Boolean getReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }
    // Optional: giveHint() und guessWord() als Platzhalter für spätere Logik
}
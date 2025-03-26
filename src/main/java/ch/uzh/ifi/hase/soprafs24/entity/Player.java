
package ch.uzh.ifi.hase.soprafs24.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@Document(collection = "PLAYER")
public class Player implements Serializable {

    private static final long serialVersionUID = 1L;

    @ManyToOne
    private Team team;

    private String role; // "spymaster" oder "field operative"

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

    // Optional: giveHint() und guessWord() als Platzhalter für spätere Logik
}
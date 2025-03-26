
package ch.uzh.ifi.hase.soprafs24.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;

@Document(collection = "PLAYER")
public class Player extends DatabaseEntity {

    private static final long serialVersionUID = 1L;

    private Team team;

    private PlayerRole role; // "spymaster" oder "field operative"

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public PlayerRole getRole() {
        return role;
    }

    public void setRole(PlayerRole role) {
        this.role = role;
    }

    // Optional: giveHint() und guessWord() als Platzhalter für spätere Logik
}
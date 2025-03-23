
package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
        import java.io.Serializable;

@Entity
@Table(name = "PLAYER")
public class Player implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Team team;

    @Column
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
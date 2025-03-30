
package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;


@Entity
@Table(name = "PLAYER")
public class Player implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column
    private PlayerRole role;
    private Boolean ready;
    /**
     * Setter nur für Testzwecke.
     * In der Anwendung wird die ID automatisch generiert.
     */
    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
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
    public Boolean getReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }
    // Optional: giveHint() und guessWord() als Platzhalter für spätere Logik
}
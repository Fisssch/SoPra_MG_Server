package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LobbyPlayerDTO {

    private String username;
    private String role;
    private String team;
    private boolean ready;

    public LobbyPlayerDTO(String username, String role, String team, boolean ready) {
        this.username = username;
        this.role = role;
        this.team = team;
        this.ready = ready;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getTeam() {
        return team;
    }

    public boolean isReady() {
        return ready;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}

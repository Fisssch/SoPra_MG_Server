package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class PlayerUpdateDTO {

    private Boolean ready;
    private String color;
    private String role;
    private Long playerId;
    
    public PlayerUpdateDTO(Long playerId, Boolean ready, String color, String role) {
        this.color = color;
        this.role = role;
        this.ready = ready;
        this.playerId = playerId;
    }

    public PlayerUpdateDTO() {
    }

    public Boolean getReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }
    
    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class RemovePlayerDTO {
    private Long playerId;

    public RemovePlayerDTO(Long playerId) {
        this.playerId = playerId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }
}

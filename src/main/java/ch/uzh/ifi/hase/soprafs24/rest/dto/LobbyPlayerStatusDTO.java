package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LobbyPlayerStatusDTO {
    private int totalPlayers;
    private int readyPlayers;

    public LobbyPlayerStatusDTO(int totalPlayers, int readyPlayers) {
        this.totalPlayers = totalPlayers;
        this.readyPlayers = readyPlayers;
    }

    public int getTotalPlayers() {
        return totalPlayers;
    }

    public int getReadyPlayers() {
        return readyPlayers;
    }
}

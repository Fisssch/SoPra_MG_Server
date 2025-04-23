package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class LobbyPlayersResponseDTO {

    private int totalPlayers;
    private int readyPlayers;
    private List<LobbyPlayerDTO> players;

    public LobbyPlayersResponseDTO(int totalPlayers, int readyPlayers, List<LobbyPlayerDTO> players) {
        this.totalPlayers = totalPlayers;
        this.readyPlayers = readyPlayers;
        this.players = players;
    }

    public int getTotalPlayers() {
        return totalPlayers;
    }

    public int getReadyPlayers() {
        return readyPlayers;
    }

    public List<LobbyPlayerDTO> getPlayers() {
        return players;
    }

    public void setTotalPlayers(int totalPlayers) {
        this.totalPlayers = totalPlayers;
    }

    public void setReadyPlayers(int readyPlayers) {
        this.readyPlayers = readyPlayers;
    }

    public void setPlayers(List<LobbyPlayerDTO> players) {
        this.players = players;
    }
}

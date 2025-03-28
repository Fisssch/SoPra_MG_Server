package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LobbyResponseDTO {
    private Long lobbyId;
    private String lobbyName;
    private String gameMode;

    public LobbyResponseDTO() {}

    public LobbyResponseDTO(Long lobbyId, String lobbyName, String gameMode) {
        this.lobbyId = lobbyId;
        this.lobbyName = lobbyName;
        this.gameMode = gameMode;
    }

    public Long getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(Long lobbyId) {
        this.lobbyId = lobbyId;
    }

    public String getLobbyName() {
        return lobbyName;
    }

    public void setLobbyName(String lobbyName) {
        this.lobbyName = lobbyName;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }
}
package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LobbyPostDTO {
    private String lobbyName;
    private String gameMode;
    private Long hostId;
    private Integer lobbyCode;

    private boolean openForLostPlayers;

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

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public Integer getLobbyCode() {
        return lobbyCode;
    }

    public void setLobbyCode(Integer lobbyCode) {
        this.lobbyCode = lobbyCode;
    }

    public boolean isOpenForLostPlayers() {
        return openForLostPlayers;
    }

    public void setOpenForLostPlayers(boolean openForLostPlayers) {
        this.openForLostPlayers = openForLostPlayers;
    }
}
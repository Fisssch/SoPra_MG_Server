package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LobbyResponseDTO {
    private Long id;
    private String lobbyName;
    private String gameMode;

    private Integer lobbyCode;



    public LobbyResponseDTO(Long id, String lobbyName, String gameMode, Integer lobbyCode) {
        this.id = id;
        this.lobbyName = lobbyName;
        this.gameMode = gameMode;
        this.lobbyCode = lobbyCode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
    public Integer getLobbyCode() {
        return lobbyCode;
    }

    public void setLobbyCode(Integer lobbyCode) {
        this.lobbyCode = lobbyCode;
    }
}
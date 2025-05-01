package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class GetLobbyDTO {
    public Long id;
    public String lobbyName;
    public String gameMode;
    public Integer lobbyCode;
    public List<String> customWords;
    private boolean openForLostPlayers;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLobbyName() { return lobbyName; }
    public void setLobbyName(String lobbyName) { this.lobbyName = lobbyName; }

    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }

    public Integer getLobbyCode() { return lobbyCode; }
    public void setLobbyCode(Integer lobbyCode) { this.lobbyCode = lobbyCode; }

    public List<String> getCustomWords() { return customWords; }
    public void setCustomWords(List<String> customWords) { this.customWords = customWords; }

    public boolean isOpenForLostPlayers() { return openForLostPlayers; }
    public void setOpenForLostPlayers(boolean openForLostPlayers) { this.openForLostPlayers = openForLostPlayers; }
}


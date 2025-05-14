package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.time.Instant;

public class LobbyResponseDTO {
    private Long id;
    private String lobbyName;
    private String gameMode;

    private Integer lobbyCode;

    private Instant createdAt;
    private String language; 
    private boolean openForLostPlayers;
    private Integer turnDuration;

    public LobbyResponseDTO(Long id, String lobbyName, String gameMode, Integer lobbyCode, Instant createdAt, String language, Boolean openForLostPlayers, Integer turnDuration) {
        this.id = id;
        this.lobbyName = lobbyName;
        this.gameMode = gameMode;
        this.lobbyCode = lobbyCode;
        this.createdAt = createdAt;
        this.language = language;
        this.openForLostPlayers = openForLostPlayers;
        this.turnDuration = turnDuration;
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
    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isOpenForLostPlayers() {
        return openForLostPlayers;
    }

    public void setOpenForLostPlayers(boolean openForLostPlayers) {
        this.openForLostPlayers = openForLostPlayers;
    }

    public String getLanguage() {
        return language;
    }
    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getTurnDuration() {
        return turnDuration;
    }
    public void setTurnDuration(Integer turnDuration) {
        this.turnDuration = turnDuration;
    }
}
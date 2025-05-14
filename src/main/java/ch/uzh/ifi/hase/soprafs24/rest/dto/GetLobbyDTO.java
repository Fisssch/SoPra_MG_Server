package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class GetLobbyDTO {
    public Long id;
    public String lobbyName;
    public String gameMode;    
    public Integer lobbyCode;
    public List<String> customWords; 
    public Integer turnDuration;
}

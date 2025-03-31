package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;

public class GameStartDTO {
    private TeamColor startingTeam;
    private GameMode gameMode; 
    private String theme;

    public TeamColor getStartingTeam() {
        return startingTeam;
    }

    public void setStartingTeam(TeamColor startingTeam) {
        this.startingTeam = startingTeam;
    }

    public GameMode getGameMode(){
        return gameMode;
    }

    public void setGameMode(GameMode gameMode){
        this.gameMode = gameMode;
    }

    public String getTheme(){
        return theme;
    }

    public void setTheme(String theme){
        this.theme = theme;
    }
}

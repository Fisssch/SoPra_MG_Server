package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;

public class GameStartDTO {
    private TeamColor startingTeam;
    private GameMode gameMode; 

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
}

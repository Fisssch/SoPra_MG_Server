package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class makeGuessDTO {
    public makeGuessDTO(){}
    
    public makeGuessDTO(String teamColor, String wordStr) {
        this.teamColor = teamColor;
        this.wordStr = wordStr;
    }

    private String teamColor;
    private String wordStr;

    public String getTeamColor() {
        return teamColor;
    }

    public void setTeamColor(String teamColor) {
        this.teamColor = teamColor;
    }

    public String getWordStr() {
        return wordStr;
    }

    public void setWordStr(String wordStr) {
        this.wordStr = wordStr;
    }
}

package ch.uzh.ifi.hase.soprafs24.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;

import java.util.List;
import java.util.Map;

@Document(collection = "GAME")
public class Game extends DatabaseEntity {

    private List<String> words; 
    private List<Card> board; 
    private TeamColor startingTeam; 
    private TeamColor teamTurn; 
    private String status;
    private String winningTeam;
    private GameMode gameMode;
    private String currentHint;
    private int wordCount;
    private int guessedInHint;

    public GameMode getGameMode(){
        return gameMode;
    }

    public void setGameMode(GameMode gameMode){
        this.gameMode = gameMode;
    }

    public String getWinningTeam(){
        return winningTeam;
    }

    public void setWinningTeam(String winningTeam){
        this.winningTeam = winningTeam;
    }

    public String getStatus(){
        return status;
    }

    public void setStatus(String status){
        this.status = status;
    }

    public TeamColor getTeamTurn(){
        return teamTurn;
    }

    public void setTeamTurn(TeamColor teamTurn){
        this.teamTurn = teamTurn;
    }

    public List<String> getWords(){
        return words;
    }

    public void setWords(List<String> words){
        this.words = words;
    }

    public TeamColor getStartingTeam(){
        return startingTeam;
    }

    public void setStartingTeam(TeamColor startingColor){
        this.startingTeam = startingColor; 
    }

    public List<Card> getBoard(){
        return board;
    }

    public void setBoard(List<Card> board){
        this.board = board; 
    }

    public Map.Entry<String, Integer> getCurrentHint() {
        if (currentHint == null || wordCount == 0) {
            return null; 
        }
        return Map.entry(currentHint, wordCount);
    }

    public void setCurrentHint(String currentHint, Integer wordCount) {
        this.currentHint = currentHint;
        this.wordCount = wordCount;
        this.guessedInHint = 0;
    }

    public int getGuessedInHint() {
        return guessedInHint;
    }

    public void setGuessedInHint(int guessedInHint) {
        this.guessedInHint = guessedInHint;
    }
}

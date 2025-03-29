package ch.uzh.ifi.hase.soprafs24.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;

import java.util.List;
import java.util.Map;

@Document(collection = "GAME")
public class Game extends DatabaseEntity {

    private List<String> words; 
    private List<Card> board; 
    private TeamColor startingTeam; 

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


}
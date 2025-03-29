package ch.uzh.ifi.hase.soprafs24.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "GAME")
public class Game extends DatabaseEntity {

    private List<String> words; 

    public List<String> getWords(){
        return words;
    }

    public void setWords(List<String> words){
        this.words = words;
    }
}
package ch.uzh.ifi.hase.soprafs24.entity;

import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Map;

@Document(collection = "GAME")
public class Game extends DatabaseEntity {
    private String currentHint;
    private int wordCount;
    private Team guessingTeam;

    public Map.Entry<String, Integer> getCurrentHint() {
        return Map.entry(currentHint, wordCount);
    }

    public void setCurrentHint(String currentHint, Integer wordCount) {
        this.currentHint = currentHint;
        this.wordCount = wordCount;
    }

    public Team getGuessingTeam() {
        return guessingTeam;
    }

    public void setGuessingTeam(Team guessingTeam) {
        this.guessingTeam = guessingTeam;
    }
}

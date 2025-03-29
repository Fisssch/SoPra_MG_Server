package ch.uzh.ifi.hase.soprafs24.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;

@Document(collection = "LOBBY")
public class Lobby extends DatabaseEntity {
    public GameMode gameMode;

    public void setGameMode(GameMode gameMode) {
        if (gameMode != null)
            this.gameMode = gameMode;
    }

    public GameMode getGameMode() {
        return gameMode;
    }
}

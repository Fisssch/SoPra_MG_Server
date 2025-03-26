package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;

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

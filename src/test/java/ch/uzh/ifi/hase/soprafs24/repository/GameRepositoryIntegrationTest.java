package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Import(RepositoryConfiguration.class)
public class GameRepositoryIntegrationTest {

    @Autowired
    private GameRepository gameRepository;

    @Test
    void insertAndFindGame_success() {
        Game game = new Game();
        game.setId(2000L);
        game.setGameMode(GameMode.CLASSIC);
        game.setStatus("IN_PROGRESS");
        game.setStartingTeam(TeamColor.RED);
        game.setTurnDuration(45);

        gameRepository.save(game);

        Optional<Game> found = gameRepository.findById(2000L);
        assertTrue(found.isPresent());
        assertEquals(GameMode.CLASSIC, found.get().getGameMode());
        assertEquals("IN_PROGRESS", found.get().getStatus());
        assertEquals(TeamColor.RED, found.get().getStartingTeam());
        assertEquals(45, found.get().getTurnDuration());

        gameRepository.deleteById(2000L); // Cleanup
    }
}
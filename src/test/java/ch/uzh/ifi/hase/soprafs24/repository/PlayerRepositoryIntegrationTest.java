package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Import(RepositoryConfiguration.class)
public class PlayerRepositoryIntegrationTest {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Test
    void insertAndFindByTeamId_success() {
        // Zuerst ein Team erstellen und speichern
        Team team = new Team();
        team.setId(101L);
        team.setColor(TeamColor.BLUE);
        teamRepository.save(team);

        // Player mit gesetztem Team
        Player player = new Player();
        player.setId(1000L);
        player.setTeam(team);
        playerRepository.save(player);

        // Test: alle Spieler mit Team-ID abrufen
        List<Player> foundPlayers = playerRepository.findByTeamId(101L);
        assertFalse(foundPlayers.isEmpty());
        assertNotNull(foundPlayers.get(0).getTeam());
        assertEquals(101L, foundPlayers.get(0).getTeam().getId());

        // Aufräumen
        playerRepository.deleteById(1000L);
        teamRepository.deleteById(101L);
    }
}
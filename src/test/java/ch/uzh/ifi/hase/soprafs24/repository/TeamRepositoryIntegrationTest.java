package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs24.entity.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Import(RepositoryConfiguration.class)
public class TeamRepositoryIntegrationTest {

    @Autowired
    private TeamRepository teamRepository;

    @Test
    void insertAndFindByColor_success() {
        // Team vorbereiten
        Team team = new Team();
        team.setId(123L);
        team.setColor(TeamColor.RED);
        teamRepository.save(team);

        // Methode testen
        List<Team> redTeams = teamRepository.findByColor("RED");
        assertFalse(redTeams.isEmpty());
        assertEquals(TeamColor.RED, redTeams.get(0).getColor());

        // Aufräumen
        teamRepository.deleteById(123L);
    }
}
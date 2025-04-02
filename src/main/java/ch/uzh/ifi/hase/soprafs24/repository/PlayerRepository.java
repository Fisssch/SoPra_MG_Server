package ch.uzh.ifi.hase.soprafs24.repository;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs24.entity.Player;

import java.util.List;

@Repository
public interface PlayerRepository extends CustomMongoRepository<Player> {
    /**
     * Find all players that belong to a team with the given ID
     * @param teamId the ID of the team
     * @return list of players that belong to the team
     */
    @Query("{ 'team.$id' : ?0 }")
    List<Player> findByTeamId(Long teamId);
}

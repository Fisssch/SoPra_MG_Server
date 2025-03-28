package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Team;
import java.util.List;

import org.springframework.data.mongodb.repository.Query;


public interface TeamRepository extends CustomMongoRepository<Team> {
    @Query("{ 'color' : ?0 }")
    List<Team> findByColor(String color);
}

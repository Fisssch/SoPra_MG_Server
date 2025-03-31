package ch.uzh.ifi.hase.soprafs24.repository;

import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs24.entity.Player;

@Repository
public interface PlayerRepository extends CustomMongoRepository<Player> {
    
}

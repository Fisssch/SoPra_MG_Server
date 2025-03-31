package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LobbyRepository extends CustomMongoRepository<Lobby> {
    @Query("{'lobbyCode': ?0}")
    Lobby findByLobbyCode(Integer lobbyCode);
}
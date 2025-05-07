package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LobbyRepository extends CustomMongoRepository<Lobby> {

    @Query("{'lobbyCode': ?0}")
    Optional<Lobby> findByLobbyCode(Integer lobbyCode);

    @Query("{'openForLostPlayers': true, '_id': { '$gte': 20 }}")
    List<Lobby> findOpenLobbiesForLostPlayers();
}
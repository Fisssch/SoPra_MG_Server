package ch.uzh.ifi.hase.soprafs24.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;

@Repository
public interface LobbyRepository extends MongoRepository<Lobby, String> {
    Lobby findByLobbyCode(Integer lobbyCode);
}
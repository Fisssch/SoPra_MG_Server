package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static com.mongodb.internal.connection.tlschannel.util.Util.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataMongoTest
@Import(RepositoryConfiguration.class) // falls notwendig
public class LobbyRepositoryIntegrationTest {

    @Autowired
    private LobbyRepository lobbyRepository;

    @Test
    void insertAndFindLobby_success() {
        Lobby lobby = new Lobby();
        lobby.setId(999L); // <-- wichtig!
        lobby.setLobbyName("TestLobby");
        lobby.setLobbyCode(1234);

        lobbyRepository.save(lobby);

        Optional<Lobby> found = lobbyRepository.findByLobbyCode(1234);
        assertTrue(found.isPresent());
        assertEquals("TestLobby", found.get().getLobbyName());

        lobbyRepository.deleteById(999L); // aufräumen
    }
}

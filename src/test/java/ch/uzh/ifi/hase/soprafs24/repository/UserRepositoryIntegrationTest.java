package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Import(RepositoryConfiguration.class)
public class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void insertAndFindByUsernameAndToken_success() {
        // User erstellen
        User user = new User();
        user.setId(300L);
        user.setUsername("testuser");
        user.setToken("abc123");
        userRepository.save(user);

        // Suche per Username
        User foundByUsername = userRepository.findByUsername("testuser");
        assertNotNull(foundByUsername);
        assertEquals("abc123", foundByUsername.getToken());

        // Suche per Token
        User foundByToken = userRepository.findByToken("abc123");
        assertNotNull(foundByToken);
        assertEquals("testuser", foundByToken.getUsername());

        // Aufräumen
        userRepository.deleteById(300L);
    }
}
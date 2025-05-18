package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;

@SpringBootTest
@ActiveProfiles("test")
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    public void cleanUp() {
        User existing = userRepository.findByUsername("integrationUser");
        if (existing != null) {
            userRepository.delete(existing);
        }
    }

    @Test
    public void createUser_userIsPersistedInDatabase() {
        User user = new User();
        user.setUsername("integrationUser");
        user.setPassword("secure");

        User created = userService.createUser(user);

        User found = userRepository.findById(created.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("integrationUser", found.getUsername());
    }
}
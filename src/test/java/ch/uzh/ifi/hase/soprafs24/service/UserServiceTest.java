package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // given
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUsername");
        testUser.setPassword("testPassword");
        testUser.setToken("testToken");
        testUser.setOnlineStatus(UserStatus.ONLINE);
        testUser.setCreationDate(LocalDateTime.now());
        testUser.setWins(5);
        testUser.setLosses(3);
        testUser.setBlackCardGuesses(1);

        // when -> any object is being saved in the userRepository -> return the dummy
        // testUser
        Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
    }

    @Test
    public void createUser_validInputs_success() {
        // when
        User createdUser = userService.createUser(testUser);

        // then
        verify(userRepository).save(Mockito.any());
        assertEquals(testUser.getId(), createdUser.getId());
        assertEquals(testUser.getUsername(), createdUser.getUsername());
        assertNotNull(createdUser.getToken());
        assertEquals(UserStatus.ONLINE, createdUser.getOnlineStatus());
        assertEquals(0, createdUser.getWins());
        assertEquals(0, createdUser.getLosses());
        assertEquals(0, createdUser.getBlackCardGuesses());
    }

    @Test
    public void createUser_duplicateUsername_throwsException() {

        // given -> setup additional mocks for UserRepository
        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

        // then -> attempt to create second user with same user -> check that an error
        // is thrown
        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    }

    @Test
    public void createUser_nullUsername_throwsException() {
        // given
        testUser.setUsername(null);

        // then
        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    }

    @Test
    public void createUser_blankUsername_throwsException() {
        // given
        testUser.setUsername("  ");

        // then
        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    }

    @Nested
    class LoginTests {
        @Test
        public void loginUser_validCredentials_success() {
            // given
            when(userRepository.findByUsername("testUsername")).thenReturn(testUser);

            // when
            User loggedInUser = userService.loginUser("testUsername", "testPassword");

            // then
            assertEquals(testUser.getId(), loggedInUser.getId());
            assertEquals(testUser.getUsername(), loggedInUser.getUsername());
            assertEquals(UserStatus.ONLINE, loggedInUser.getOnlineStatus());
            assertNotNull(loggedInUser.getToken());
            verify(userRepository).save(any(User.class)); // Verify token is updated
        }

        @Test
        public void loginUser_nonExistingUser_throwsException() {
            // given
            when(userRepository.findByUsername("nonExistingUser")).thenReturn(null);

            // then
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.loginUser("nonExistingUser", "anyPassword"));
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertEquals("This username does not exist", exception.getReason());
        }

        @Test
        public void loginUser_wrongPassword_throwsException() {
            // given
            when(userRepository.findByUsername("testUsername")).thenReturn(testUser);

            // then
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.loginUser("testUsername", "wrongPassword"));
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
            assertEquals("Incorrect password", exception.getReason());
        }
    }

    @Test
    public void logoutUser_validToken_setsStatusOffline() {
        // given
        when(userRepository.findByToken("testToken")).thenReturn(testUser);

        // when
        userService.logoutUser("testToken");

        // then
        assertEquals(UserStatus.OFFLINE, testUser.getOnlineStatus());
        assertNull(testUser.getToken());
        verify(userRepository).save(testUser);
    }

    @Nested
    class TokenTests {
        @Test
        public void validateToken_validToken_returnsUser() {
            // given
            when(userRepository.findByToken("testToken")).thenReturn(testUser);

            // when
            User foundUser = userService.validateToken("testToken");

            // then
            assertEquals(testUser.getId(), foundUser.getId());
        }

        @Test
        public void validateToken_invalidToken_throwsException() {
            // given
            when(userRepository.findByToken("invalidToken")).thenReturn(null);

            // then
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.validateToken("invalidToken"));
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
            assertEquals("An invalid token was provided", exception.getReason());
        }

        @Test
        public void validateToken_nullToken_throwsException() {
            // then
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.validateToken(null));
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
            assertEquals("Missing authentication", exception.getReason());
        }

        @Test
        public void extractToken_bearerFormat_returnsToken() {
            // when
            String token = userService.extractToken("Bearer testToken");

            // then
            assertEquals("testToken", token);
        }

        @Test
        public void extractToken_plainFormat_returnsToken() {
            // when
            String token = userService.extractToken("testToken");

            // then
            assertEquals("testToken", token);
        }

        @Test
        public void extractToken_nullHeader_throwsException() {
            // then
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.extractToken(null));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertEquals("Missing header", exception.getReason());
        }

        @Test
        public void extractAndValidateToken_validToken_noException() {
            // given
            when(userRepository.findByToken("testToken")).thenReturn(testUser);

            // then -> should not throw exception
            assertDoesNotThrow(() -> userService.extractAndValidateToken("Bearer testToken"));
        }
    }

    @Nested
    class UserProfileTests {
        @Test
        public void getUsers_returnsAllUsers() {
            // given
            User user2 = new User();
            user2.setId(2L);
            when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));

            // when
            List<User> users = userService.getUsers();

            // then
            assertEquals(2, users.size());
        }

        @Test
        public void getUserById_existingUser_returnsUser() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // when
            User foundUser = userService.getUserById(1L);

            // then
            assertEquals(testUser.getId(), foundUser.getId());
        }

        @Test
        public void getUserById_nonExistingUser_throwsException() {
            // given
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            // then
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.getUserById(99L));
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertEquals("User not found", exception.getReason());
        }
    }

    @Nested
    class UsernameUpdateTests {
        @Test
        public void updateUsername_validRequest_success() {
            // given
            User currentUser = new User();
            currentUser.setId(1L);
            currentUser.setUsername("oldUsername");
            when(userRepository.findByToken("testToken")).thenReturn(currentUser);
            when(userRepository.findByUsername("newUsername")).thenReturn(null);

            // when
            userService.updateUsername(1L, "newUsername", "testToken");

            // then
            assertEquals("newUsername", currentUser.getUsername());
            verify(userRepository).save(currentUser);
        }

        @Test
        public void updateUsername_differentUserId_throwsException() {
            // given
            User currentUser = new User();
            currentUser.setId(1L);
            when(userRepository.findByToken("testToken")).thenReturn(currentUser);

            // then
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.updateUsername(2L, "newUsername", "testToken"));
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
            assertEquals("You can only update your own profile.", exception.getReason());
        }

        @Test
        public void updateUsername_emptyUsername_throwsException() {
            // given
            when(userRepository.findByToken("testToken")).thenReturn(testUser);

            // then
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.updateUsername(1L, "", "testToken"));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertEquals("Username can't be empty", exception.getReason());
        }

        @Test
        public void updateUsername_duplicateUsername_throwsException() {
            // given
            User existingUser = new User();
            existingUser.setId(2L);
            existingUser.setUsername("existingUsername");

            when(userRepository.findByToken("testToken")).thenReturn(testUser);
            when(userRepository.findByUsername("existingUsername")).thenReturn(existingUser);

            // then
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.updateUsername(1L, "existingUsername", "testToken"));
            assertEquals(HttpStatus.CONFLICT, exception.getStatus());
            assertEquals("Username already taken.", exception.getReason());
        }
    }

    @Nested
    class PasswordUpdateTests {
        @Test
        public void updatePassword_validRequest_success() {
            // given
            User currentUser = new User();
            currentUser.setId(1L);
            currentUser.setPassword("oldPassword");
            when(userRepository.findByToken("testToken")).thenReturn(currentUser);

            // when
            userService.updatePassword(1L, "oldPassword", "newPassword", "testToken");

            // then
            assertEquals("newPassword", currentUser.getPassword());
            verify(userRepository).save(currentUser);
        }

        @Test
        public void updatePassword_differentUserId_throwsException() {
            // given
            User currentUser = new User();
            currentUser.setId(1L);
            when(userRepository.findByToken("testToken")).thenReturn(currentUser);

            // then
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.updatePassword(2L, "oldPassword", "newPassword", "testToken"));
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
            assertEquals("You can only update your own profile.", exception.getReason());
        }

        @Test
        public void updatePassword_emptyNewPassword_throwsException() {
            // given
            when(userRepository.findByToken("testToken")).thenReturn(testUser);

            // then
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.updatePassword(1L, "oldPassword", "", "testToken"));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertEquals("New password cannot be empty.", exception.getReason());
        }

        @Test
        public void updatePassword_incorrectOldPassword_throwsException() {
            // given
            when(userRepository.findByToken("testToken")).thenReturn(testUser);

            // then
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.updatePassword(1L, "wrongPassword", "newPassword", "testToken"));
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
            assertEquals("Old password is incorrect.", exception.getReason());
        }
    }

    @Test
    public void extractAndValidateToken_invalidToken_throwsException() {
        when(userRepository.findByToken("invalid")).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            userService.extractAndValidateToken("Bearer invalid")
        );
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("An invalid token was provided", exception.getReason());
    }

    @Test
        public void extractToken_withOnlyBearer_returnsEmptyString() {
        String result = userService.extractToken("Bearer ");
        assertEquals("", result);
    }
}

package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

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
    testUser.setToken("validToken"); 

    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
  }

  //create user 

  @Test
  public void createUser_validInputs_success() {
    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    User createdUser = userService.createUser(testUser);

    // then
    Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

    assertEquals(testUser.getId(), createdUser.getId());
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.ONLINE, createdUser.getOnlineStatus());
  }

  @Test
  public void createUser_duplicateUsername_throwsException() {
    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

    // then -> attempt to create second user with same user -> check that an error
    // is thrown
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_nullUsername_throwsException() {
    testUser.setUsername(null);
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  //login user 

  @Test
  public void loginUser_validCredentials_success() {
    Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(testUser);

    User loggedInUser = userService.loginUser("testUsername", "testPassword");

    assertNotNull(loggedInUser.getToken());
    assertEquals(UserStatus.ONLINE, loggedInUser.getOnlineStatus());
  }

  @Test
  public void loginUser_invalidUsername_throwsException() {
    Mockito.when(userRepository.findByUsername("wrongUsername")).thenReturn(null);

    assertThrows(ResponseStatusException.class, () -> userService.loginUser("wrongUsername", "password"));
  }

  @Test
  public void loginUser_invalidPassword_throwsException() {
    Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(testUser);

    assertThrows(ResponseStatusException.class, () -> userService.loginUser("testUsername", "wrongPassword"));
  }

  //logout user 

  @Test
  public void logoutUser_validToken_success() {
    Mockito.when(userRepository.findByToken("validToken")).thenReturn(testUser);

    userService.logoutUser("validToken");

    Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
    assertNull(testUser.getToken());
    assertEquals(UserStatus.OFFLINE, testUser.getOnlineStatus());
  }

  @Test
  public void logoutUser_invalidToken_throwsException() {
    Mockito.when(userRepository.findByToken("invalidToken")).thenReturn(null);

    assertThrows(ResponseStatusException.class, () -> userService.logoutUser("invalidToken"));
  }

  //update username 

  @Test
  public void updateUsername_validInputs_success() {
    Mockito.when(userRepository.findByToken("validToken")).thenReturn(testUser);

    userService.updateUsername(1L, "newUsername", "validToken");

    Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
  }

  @Test
  public void updateUsername_invalidUser_throwsException() {
    Mockito.when(userRepository.findByToken("validToken")).thenReturn(testUser);

    assertThrows(ResponseStatusException.class, () -> userService.updateUsername(2L, "newUsername", "validToken"));
  }

  @Test
  public void updateUsername_duplicateUsername_throwsException() {
    // Mock logged in user
    Mockito.when(userRepository.findByToken("validToken")).thenReturn(testUser);

    // Mock that a different user already has the username
    User anotherUser = new User();
    anotherUser.setId(2L); // different id than testUser!
    anotherUser.setUsername("newUsername");
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(anotherUser);

    assertThrows(ResponseStatusException.class, () -> userService.updateUsername(1L, "newUsername", "validToken"));
  }

  @Test
  public void updateUsername_nullUsername_throwsException() {
    Mockito.when(userRepository.findByToken("validToken")).thenReturn(testUser);

    assertThrows(ResponseStatusException.class, () -> userService.updateUsername(1L, null, "validToken"));
  }

  //update password

  @Test
  public void updatePassword_validInputs_success() {
    Mockito.when(userRepository.findByToken("validToken")).thenReturn(testUser);

    userService.updatePassword(1L, "testPassword", "newPassword", "validToken");

    Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
  }

  @Test
  public void updatePassword_wrongOldPassword_throwsException() {
    Mockito.when(userRepository.findByToken("validToken")).thenReturn(testUser);

    assertThrows(ResponseStatusException.class, () -> userService.updatePassword(1L, "wrongOldPassword", "newPassword", "validToken"));
  }

  @Test
  public void updatePassword_emptyNewPassword_throwsException() {
    Mockito.when(userRepository.findByToken("validToken")).thenReturn(testUser);

    assertThrows(ResponseStatusException.class, () -> userService.updatePassword(1L, "testPassword", "", "validToken"));
  }

  @Test
  public void updatePassword_wrongUserId_throwsException() {
    Mockito.when(userRepository.findByToken("validToken")).thenReturn(testUser);

    assertThrows(ResponseStatusException.class, () -> userService.updatePassword(2L, "testPassword", "newPassword", "validToken"));
  }

  //validate token 

  @Test
  public void validateToken_validToken_success() {
    Mockito.when(userRepository.findByToken("validToken")).thenReturn(testUser);

    User user = userService.validateToken("validToken");

    assertEquals(testUser, user);
  }

  @Test
  public void validateToken_invalidToken_throwsException() {
    Mockito.when(userRepository.findByToken("invalidToken")).thenReturn(null);

    assertThrows(ResponseStatusException.class, () -> userService.validateToken("invalidToken"));
  }

  @Test
  public void validateToken_nullToken_throwsException() {
    assertThrows(ResponseStatusException.class, () -> userService.validateToken(null));
  }

}

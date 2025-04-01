package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

  private final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  //get users  
  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  //get user
  public User getUserById(Long id){
    return userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")); 
    
  }

  //register 
  public User createUser(User newUser) {
    newUser.setToken(UUID.randomUUID().toString());
    newUser.setOnlineStatus(UserStatus.ONLINE);
    newUser.setCreationDate(LocalDateTime.now());
    newUser.setWins(0);
    newUser.setLosses(0);
    newUser.setBlackCardGuesses(0);
    checkIfUserExists(newUser);
    newUser = userRepository.save(newUser);

    log.debug("Created Information for User: {}", newUser);
    return newUser;
  }

  //login
  public User loginUser(String username, String password) {
    User existingUser = userRepository.findByUsername(username); 
    if (existingUser == null){
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This username does not exist"); 
    }
    if (!existingUser.getPassword().equals(password)){
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password"); 
    }

    existingUser.setToken(UUID.randomUUID().toString());
    existingUser.setOnlineStatus(UserStatus.ONLINE);
    userRepository.save(existingUser);
    return existingUser;
  }

  //logout 
  public void logoutUser(String token) {
    User user = validateToken(token); 
    user.setOnlineStatus(UserStatus.OFFLINE);
    user.setToken(null);
    userRepository.save(user);
  }

  //update username 
  public void updateUsername(Long id, String newUsername, String token){
    User user = validateToken(token); 

    if (!user.getId().equals(id)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own profile.");
  }

  if (newUsername == null || newUsername.trim().isEmpty()){
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username can't be empty");
  }

  User existingUser = userRepository.findByUsername(newUsername);
    if (existingUser != null && !existingUser.getId().equals(id)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken.");
    }

  user.setUsername(newUsername);
  userRepository.save(user); 
  }

  //update password 
  public void updatePassword(Long id, String oldPassword, String newPassword, String token) {
    User user = validateToken(token);

    if (!user.getId().equals(id)) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You can only update your own profile.");
    }

    if (newPassword == null || newPassword.trim().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be empty.");
    }

    if (!user.getPassword().equals(oldPassword)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Old password is incorrect."); 
    }

    user.setPassword(newPassword);
    userRepository.save(user);
}


  //////////////////// helper methods: ////////////////////

  /**
   * This is a helper method that will check the uniqueness criteria of the
   * username and the name
   * defined in the User entity. The method will do nothing if the input is unique
   * and throw an error otherwise.
   *
   * @param userToBeCreated
   * @throws org.springframework.web.server.ResponseStatusException
   * @see User
   */
  private void checkIfUserExists(User userToBeCreated) {
    String username = userToBeCreated.getUsername(); 
    if (username == null || username.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username can't be null or blank!"); 
    }
    
    User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
    if (userByUsername != null) {
      String errorMessage = String.format(
          "The username '%s' is already taken. User could not be created!",
          userToBeCreated.getUsername()
      );
      throw new ResponseStatusException(HttpStatus.CONFLICT,errorMessage);
    } 
  }

  public User validateToken(String token){
    if (token == null || token.isEmpty()){
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication");
    }
    User user = userRepository.findByToken(token); 
    if (user == null){
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "An invalid token was provided");
    }
    return user;
  } 

  public void extractAndValidateToken(String header) {
    String token = extractToken(header); 
    validateToken(token); 
  }

  public String extractToken(String header){
    if (header == null){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing header");
    }
    if (header.startsWith("Bearer ")) {
      header = header.substring(7); // Remove "Bearer " prefix
    }
    return header;
  }
}

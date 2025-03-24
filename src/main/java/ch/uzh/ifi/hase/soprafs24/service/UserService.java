package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

  @Autowired
  public UserService(@Qualifier("userRepository") UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  //register 
  public User createUser(User newUser) {
    newUser.setToken(UUID.randomUUID().toString());
    newUser.setOnlineStatus(UserStatus.ONLINE);
    checkIfUserExists(newUser);
    // saves the given entity but data is only persisted in the database once
    // flush() is called
    newUser = userRepository.save(newUser);
    userRepository.flush();

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
    return existingUser;
  }

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
}

package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.annotation.AuthorizationRequired;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserLoginDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserUpdatePasswordDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserUpdateUsernameDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */


@RestController
public class UserController {

  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }

  //update username 
  @PutMapping("/users/{id}/username")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateUsername(
          @PathVariable Long id, 
          @RequestHeader("Authorization") String authHeader, 
          @RequestBody UserUpdateUsernameDTO usernameDTO) {
    String token = userService.extractToken(authHeader); 
    userService.updateUsername(id, usernameDTO.getUsername(), token); 
  }

  //update password 
  @PutMapping("/users/{id}/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updatePassword(
          @PathVariable Long id, 
          @RequestHeader("Authorization") String authHeader, 
          @RequestBody UserUpdatePasswordDTO passwordDTO) {
    String token = userService.extractToken(authHeader); 
    userService.updatePassword(id, passwordDTO.getOldPassword(), passwordDTO.getNewPassword(), token); 
  }


  //user stats 
  @GetMapping("/users/{id}")
  @ResponseStatus(HttpStatus.OK)
  public UserGetDTO getUser(@RequestHeader("Authorization") String authHeader, @PathVariable Long id){
    String token = userService.extractToken(authHeader); 
    userService.validateToken(token); 
    User user = userService.getUserById(id); 
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user); 
  }

  //users overview 
  @GetMapping("/users")
  @ResponseStatus(HttpStatus.OK)
  @AuthorizationRequired
  public List<UserGetDTO> getAllUsers() {
    // fetch all users in the internal representation
    List<User> users = userService.getUsers();
    List<UserGetDTO> userGetDTOs = new ArrayList<>();

    // convert each user to the API representation
    for (User user : users) {
      userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
    }
    return userGetDTOs;
  }

  //register  
  @PostMapping("/users")
  public ResponseEntity<UserGetDTO> createUser(@RequestBody UserPostDTO userPostDTO) {
    // convert API user to internal representation
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
    // create user
    User createdUser = userService.createUser(userInput);
    // convert internal representation of user back to API
    String token = createdUser.getToken();
    UserGetDTO userDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
    
    return ResponseEntity
        .status(HttpStatus.CREATED) //201
        .header("Authorization", "Bearer " + token)
        .body(userDTO);
  }

  //login 
  @PostMapping("/users/login")
  public ResponseEntity<UserGetDTO> loginUser(@RequestBody UserLoginDTO userLoginDTO) {
    User authUser = userService.loginUser(userLoginDTO.getUsername(), userLoginDTO.getPassword()); 
    String token = authUser.getToken();
    UserGetDTO userDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(authUser);

    return ResponseEntity
        .status(HttpStatus.OK) //200
        .header("Authorization", "Bearer " + token)
        .body(userDTO);
  }

  //logout
  @PostMapping("/users/logout")
  @ResponseStatus(HttpStatus.OK)
  public Map<String, String> logoutUser(@RequestHeader("Authorization") String authHeader) {

    String token = userService.extractToken(authHeader);
    userService.logoutUser(token); 
    return Map.of("message", "Successfully logged out");
  }
}

package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserLoginDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserLogoutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
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

@CrossOrigin(
  origins = "http://localhost:3000", 
  exposedHeaders = "Authorization"
) 
@RestController
public class UserController {

  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }

  //users overview 
  @GetMapping("/users")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UserGetDTO> getAllUsers(@RequestHeader("Authorization") String authHeader) {
    String token = userService.extractToken(authHeader);
    userService.validateToken(token);
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
  @ResponseBody
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
  @ResponseBody 
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
  @ResponseBody
  public ResponseEntity<Map<String, String>> logoutUser( @RequestHeader("Authorization") String authHeader) {

    String token = userService.extractToken(authHeader);
    userService.logoutUser(token); 
    return ResponseEntity.ok(Map.of("message", "Successfully logged out"));
  }
}

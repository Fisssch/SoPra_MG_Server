package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserLoginDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;


/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @Test
  public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
    // given
    User user = new User();
    user.setUsername("firstname@lastname");
    user.setOnlineStatus(UserStatus.OFFLINE);

    List<User> allUsers = Collections.singletonList(user);

    // this mocks the UserService -> we define above what the userService should
    // return when getUsers() is called
    given(userService.getUsers()).willReturn(allUsers);
    given(userService.validateToken(Mockito.any())).willReturn(user);

    // when
    MockHttpServletRequestBuilder getRequest = get("/users").header("Authorization", "123").contentType(MediaType.APPLICATION_JSON);

    // then
    mockMvc.perform(getRequest).andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].username", is(user.getUsername())))
        .andExpect(jsonPath("$[0].onlineStatus", is(user.getOnlineStatus().toString())));
  }

  @Test
  public void createUser_validInput_userCreated() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setUsername("testUsername");
    user.setToken("1");
    user.setOnlineStatus(UserStatus.ONLINE);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setUsername("testUsername");
    userPostDTO.setPassword("testPassword");

    given(userService.createUser(Mockito.any())).willReturn(user);

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));

    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isCreated())
        .andExpect(header().string("Authorization", "Bearer 1"))
        .andExpect(jsonPath("$.id", is(user.getId().intValue())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.onlineStatus", is(user.getOnlineStatus().toString())));
  }

  @Test
  public void loginUser_validInput_loginSuccessful() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setUsername("username");
    user.setToken("1");

    UserLoginDTO loginDTO = new UserLoginDTO();
    loginDTO.setUsername("username");
    loginDTO.setPassword("password");

    given(userService.loginUser(Mockito.any(), Mockito.any())).willReturn(user);

    MockHttpServletRequestBuilder postRequest = post("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(loginDTO));

    // when/then
    mockMvc.perform(postRequest)
            .andExpect(status().isOk())
            .andExpect(header().string("Authorization", "Bearer 1"))
            .andExpect(jsonPath("$.id", is(user.getId().intValue())))
            .andExpect(jsonPath("$.username", is(user.getUsername())));
  }

  @Test
  public void loginUser_invalidCredentials_returnsUnauthorized() throws Exception {
    UserLoginDTO loginDTO = new UserLoginDTO();
    loginDTO.setUsername("wrongUser");
    loginDTO.setPassword("wrongPass");

    given(userService.loginUser(Mockito.any(), Mockito.any()))
        .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

    MockHttpServletRequestBuilder postRequest = post("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(loginDTO));

    mockMvc.perform(postRequest)
          .andExpect(status().isUnauthorized());
  }

  @Test
  public void logoutUser_validToken_logoutSuccessful() throws Exception {
    // given
    doNothing().when(userService).logoutUser(Mockito.any()); //use do nothing here because logoutUser is a void method --> no return 

    MockHttpServletRequestBuilder postRequest = post("/users/logout")
            .header("Authorization", "Bearer 1");

    // when/then
    mockMvc.perform(postRequest)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message", is("Successfully logged out")));
  }

  @Test
  public void logoutUser_invalidToken_returnsUnauthorized() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"))
        .when(userService).logoutUser(Mockito.any());

    MockHttpServletRequestBuilder postRequest = post("/users/logout")
        .header("Authorization", "Bearer invalid");

    mockMvc.perform(postRequest)
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void getUser_validInput_userReturned() throws Exception {
    // given
    User user = new User();
    user.setId(3L);
    user.setUsername("fetchUser");

    given(userService.getUserById(3L)).willReturn(user);
    given(userService.validateToken(Mockito.any())).willReturn(user);

    MockHttpServletRequestBuilder getRequest = get("/users/3")
            .header("Authorization", "Bearer someToken");

    // when/then
    mockMvc.perform(getRequest)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is(user.getUsername())));
  }

  @Test
  public void getUser_invalidToken_returnsUnauthorized() throws Exception {
    // given
    given(userService.validateToken(Mockito.any()))
        .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

    // when
    MockHttpServletRequestBuilder getRequest = get("/users/1")
            .header("Authorization", "Bearer invalidToken");

    // then
    mockMvc.perform(getRequest)
            .andExpect(status().isUnauthorized());
  }

  @Test
  public void updateUsername_validInput_usernameUpdated() throws Exception {
    // given
    doNothing().when(userService).updateUsername(Mockito.eq(1L), Mockito.eq("newUsername"), Mockito.any());

    MockHttpServletRequestBuilder putRequest = put("/users/1/username")
            .header("Authorization", "Bearer 1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"newUsername\"}");

    // when/then
    mockMvc.perform(putRequest)
            .andExpect(status().isNoContent());
  }

  @Test
  public void updateUsername_invalidToken_returnsUnauthorized() throws Exception {

    Mockito.doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"))
        .when(userService).updateUsername(Mockito.eq(1L), Mockito.eq("newUsername"), Mockito.any());

    MockHttpServletRequestBuilder putRequest = put("/users/1/username")
        .header("Authorization", "Bearer invalid")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"newUsername\"}");

    mockMvc.perform(putRequest)
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void updatePassword_validInput_passwordUpdated() throws Exception {
    doNothing().when(userService).updatePassword(Mockito.eq(1L), Mockito.eq("oldPassword"), Mockito.eq("newPassword"), Mockito.any());

    MockHttpServletRequestBuilder putRequest = put("/users/1/password")
        .header("Authorization", "Bearer 1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"oldPassword\":\"oldPassword\", \"newPassword\":\"newPassword\"}");

    mockMvc.perform(putRequest)
        .andExpect(status().isNoContent());
  }

  @Test
  public void updatePassword_invalidToken_returnsUnauthorized() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"))
        .when(userService).updatePassword(Mockito.eq(1L), Mockito.eq("oldPassword"), Mockito.eq("newPassword"), Mockito.any());

    MockHttpServletRequestBuilder putRequest = put("/users/1/password")
        .header("Authorization", "Bearer invalid")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"oldPassword\":\"oldPassword\", \"newPassword\":\"newPassword\"}");

    mockMvc.perform(putRequest)
        .andExpect(status().isUnauthorized());
  }

  /**
   * Helper Method to convert userPostDTO into a JSON string such that the input
   * can be processed
   * Input will look like this: {"name": "Test User", "username": "testUsername"}
   * 
   * @param object
   * @return string
   */
  private String asJsonString(final Object object) {
    try {
      return new ObjectMapper().writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format("The request body could not be created.%s", e.toString()));
    }
  }
}
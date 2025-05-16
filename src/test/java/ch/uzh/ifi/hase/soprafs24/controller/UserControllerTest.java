package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserLoginDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserUpdatePasswordDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserUpdateUsernameDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getAllUsers_returnsJsonArray() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setPassword("test");
        user.setToken("1");
        user.setOnlineStatus(UserStatus.ONLINE);

        List<User> allUsers = Arrays.asList(user);

        // this mocks the UserService -> we define above what the userService should
        // return when getUsers() is called
        when(userService.getUsers()).thenReturn(allUsers);

        // when
        MockHttpServletRequestBuilder getRequest = get("/users")
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(getRequest).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].username", is(user.getUsername())))
                .andExpect(jsonPath("$[0].onlineStatus", is(user.getOnlineStatus().toString())));
    }

    @Test
    public void createUser_validInput_userCreated() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUsername");
        user.setPassword("testPassword");
        user.setToken("1");
        user.setOnlineStatus(UserStatus.ONLINE);

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUsername");
        userPostDTO.setPassword("testPassword");

        when(userService.createUser(any(User.class))).thenReturn(user);

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder postRequest = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(header().string("Authorization", "Bearer 1"))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.onlineStatus", is(user.getOnlineStatus().toString())));
    }

    @Test
    public void loginUser_validCredentials_returnsUser() throws Exception {
        // given
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("testUser");
        loginDTO.setPassword("password123");

        User authenticatedUser = new User();
        authenticatedUser.setId(1L);
        authenticatedUser.setUsername("testUser");
        authenticatedUser.setPassword("password123");
        authenticatedUser.setToken("test-token");
        authenticatedUser.setOnlineStatus(UserStatus.ONLINE);

        when(userService.loginUser("testUser", "password123")).thenReturn(authenticatedUser);

        // when/then
        mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", "Bearer test-token"))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testUser")))
                .andExpect(jsonPath("$.onlineStatus", is("ONLINE")));
    }

    @Test
    public void loginUser_invalidCredentials_returns401() throws Exception {
        // given
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("testUser");
        loginDTO.setPassword("wrongPassword");

        when(userService.loginUser("testUser", "wrongPassword"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password"));

        // when/then
        mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(loginDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void logoutUser_validToken_returns200() throws Exception {
        // given
        String token = "test-token";
        when(userService.extractToken("Bearer " + token)).thenReturn(token);
        doNothing().when(userService).logoutUser(token);

        // when/then
        mockMvc.perform(post("/users/logout")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Successfully logged out")));

        verify(userService).extractToken("Bearer " + token);
        verify(userService).logoutUser(token);
    }

    @Test
    public void getUser_validIdAndToken_returnsUser() throws Exception {
        // given
        Long userId = 1L;
        String token = "test-token";

        User user = new User();
        user.setId(userId);
        user.setUsername("testUser");
        user.setOnlineStatus(UserStatus.ONLINE);
        user.setWins(5);
        user.setLosses(3);

        when(userService.extractToken("Bearer " + token)).thenReturn(token);
        when(userService.validateToken(token)).thenReturn(user);
        when(userService.getUserById(userId)).thenReturn(user);

        // when/then
        mockMvc.perform(get("/users/" + userId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testUser")))
                .andExpect(jsonPath("$.onlineStatus", is("ONLINE")))
                .andExpect(jsonPath("$.wins", is(5)))
                .andExpect(jsonPath("$.losses", is(3)));
    }

    @Test
    public void updateUsername_validRequest_returnsNoContent() throws Exception {
        // given
        Long userId = 1L;
        String token = "test-token";
        String newUsername = "newUsername";

        UserUpdateUsernameDTO usernameDTO = new UserUpdateUsernameDTO();
        usernameDTO.setUsername(newUsername);

        when(userService.extractToken("Bearer " + token)).thenReturn(token);
        doNothing().when(userService).updateUsername(userId, newUsername, token);

        // when/then
        mockMvc.perform(put("/users/" + userId + "/username")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(usernameDTO)))
                .andExpect(status().isNoContent());

        verify(userService).updateUsername(userId, newUsername, token);
    }

    @Test
    public void updateUsername_conflictingUsername_returns409() throws Exception {
        // given
        Long userId = 1L;
        String token = "test-token";
        String existingUsername = "existingUser";

        UserUpdateUsernameDTO usernameDTO = new UserUpdateUsernameDTO();
        usernameDTO.setUsername(existingUsername);

        when(userService.extractToken("Bearer " + token)).thenReturn(token);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken."))
                .when(userService).updateUsername(userId, existingUsername, token);

        // when/then
        mockMvc.perform(put("/users/" + userId + "/username")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(usernameDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    public void updatePassword_validRequest_returnsNoContent() throws Exception {
        // given
        Long userId = 1L;
        String token = "test-token";
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";

        UserUpdatePasswordDTO passwordDTO = new UserUpdatePasswordDTO();
        passwordDTO.setOldPassword(oldPassword);
        passwordDTO.setNewPassword(newPassword);

        when(userService.extractToken("Bearer " + token)).thenReturn(token);
        doNothing().when(userService).updatePassword(userId, oldPassword, newPassword, token);

        // when/then
        mockMvc.perform(put("/users/" + userId + "/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(passwordDTO)))
                .andExpect(status().isNoContent());

        verify(userService).updatePassword(userId, oldPassword, newPassword, token);
    }

    @Test
    public void updatePassword_incorrectOldPassword_returns403() throws Exception {
        // given
        Long userId = 1L;
        String token = "test-token";
        String wrongPassword = "wrongPassword";
        String newPassword = "newPassword";

        UserUpdatePasswordDTO passwordDTO = new UserUpdatePasswordDTO();
        passwordDTO.setOldPassword(wrongPassword);
        passwordDTO.setNewPassword(newPassword);

        when(userService.extractToken("Bearer " + token)).thenReturn(token);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Old password is incorrect."))
                .when(userService).updatePassword(userId, wrongPassword, newPassword, token);

        // when/then
        mockMvc.perform(put("/users/" + userId + "/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(passwordDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void updatePassword_differentUserId_returns401() throws Exception {
        // given
        Long userId = 1L;
        String token = "test-token";
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";

        UserUpdatePasswordDTO passwordDTO = new UserUpdatePasswordDTO();
        passwordDTO.setOldPassword(oldPassword);
        passwordDTO.setNewPassword(newPassword);

        when(userService.extractToken("Bearer " + token)).thenReturn(token);
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You can only update your own profile."))
                .when(userService).updatePassword(userId, oldPassword, newPassword, token);

        // when/then
        mockMvc.perform(put("/users/" + userId + "/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(passwordDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
public void getAllUsers_withAuthorizationHeader_returnsJsonArray() throws Exception {
    User user = new User();
    user.setId(1L);
    user.setUsername("testUser");
    user.setPassword("test");
    user.setToken("token123");
    user.setOnlineStatus(UserStatus.ONLINE);

    List<User> users = List.of(user);

    when(userService.getUsers()).thenReturn(users);

    mockMvc.perform(get("/users")
            .header("Authorization", "Bearer token123")  // Add this header
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(1)))
            .andExpect(jsonPath("$[0].username", is("testUser")))
            .andExpect(jsonPath("$[0].onlineStatus", is("ONLINE")));
}
    @Nested
    class UserErrorCases {

        @Test
        public void getUser_userNotFound_returns404() throws Exception {
            Long userId = 999L;
            String token = "test-token";

            when(userService.extractToken("Bearer " + token)).thenReturn(token);
            when(userService.validateToken(token)).thenReturn(new User());
            when(userService.getUserById(userId))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            mockMvc.perform(get("/users/" + userId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void updateUsername_blankUsername_returns400() throws Exception {
            Long userId = 1L;
            String token = "test-token";

            UserUpdateUsernameDTO dto = new UserUpdateUsernameDTO();
            dto.setUsername("");

            when(userService.extractToken("Bearer " + token)).thenReturn(token);
            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be blank"))
                    .when(userService).updateUsername(eq(userId), eq(""), eq(token));

            mockMvc.perform(put("/users/" + userId + "/username")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void updatePassword_missingFields_returns400() throws Exception {
            Long userId = 1L;
            String token = "test-token";

            UserUpdatePasswordDTO dto = new UserUpdatePasswordDTO();
            // Felder leer

            when(userService.extractToken("Bearer " + token)).thenReturn(token);
            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing password fields"))
                    .when(userService).updatePassword(eq(userId), eq(null), eq(null), eq(token));

            mockMvc.perform(put("/users/" + userId + "/password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }
    @Test
    public void createUser_conflictUsername_returns409() throws Exception {
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("existingUser");
        userPostDTO.setPassword("pass");

        when(userService.createUser(any(User.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists"));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    public void createUser_missingFields_returns400() throws Exception {
        UserPostDTO dto = new UserPostDTO();
        // No username or password

        when(userService.createUser(any(User.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing fields"));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isBadRequest());
    }
}
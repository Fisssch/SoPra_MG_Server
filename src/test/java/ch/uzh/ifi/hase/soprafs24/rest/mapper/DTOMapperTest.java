package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.constant.GameLanguage;
import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.Team;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.RoleUpdateDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.PlayerUpdateDTO;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

/**
 * DTOMapperTest
 * Tests if the mapping between the internal and the external/API representation
 * works.
 */
public class DTOMapperTest {
  @Test
  public void testCreateUser_fromUserPostDTO_toUser_success() {
    // create UserPostDTO
    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setUsername("username");

    // MAP -> Create user
    User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

    // check content
    assertEquals(userPostDTO.getUsername(), user.getUsername());
  }

  @Test
  public void testGetUser_fromUser_toUserGetDTO_success() {
    // create User
    User user = new User();
    user.setUsername("firstname@lastname");
    user.setOnlineStatus(UserStatus.OFFLINE);
    user.setToken("1");

    // MAP -> Create UserGetDTO
    UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

    // check content
    assertEquals(user.getId(), userGetDTO.getId());
    assertEquals(user.getUsername(), userGetDTO.getUsername());
    assertEquals(user.getOnlineStatus(), userGetDTO.getOnlineStatus());
  }

  @Test
  public void testMapLobbyToLobbyResponseDTO_success() {
    Lobby lobby = new Lobby();
    lobby.setId(42L);
    lobby.setLobbyName("Mapper Test Lobby");
    lobby.setLobbyCode(4321);
    lobby.setGameMode(GameMode.CLASSIC);
    lobby.setCreatedAt(Instant.now());
    lobby.setCustomWords(List.of("alpha", "beta", "gamma"));
    lobby.setLanguage(GameLanguage.GERMAN);
    lobby.setOpenForLostPlayers(true);
    lobby.setTurnDuration(30);

    LobbyResponseDTO dto = DTOMapper.INSTANCE.convertEntityToLobbyResponseDTO(lobby);

    assertEquals(lobby.getId(), dto.getId());
    assertEquals(lobby.getLobbyName(), dto.getLobbyName());
    assertEquals(lobby.getLobbyCode(), dto.getLobbyCode());
    assertEquals(lobby.getGameMode().name(), dto.getGameMode());
    assertEquals(lobby.getCreatedAt(), dto.getCreatedAt());
    assertEquals(lobby.getLanguage().name(), dto.getLanguage());
    assertEquals(lobby.getTurnDuration(), dto.getTurnDuration());
  }

  @Test
  public void testConvertEntityToPlayerResponseDTO_success() {
    Player player = new Player();
    player.setId(10L);
    player.setRole(PlayerRole.FIELD_OPERATIVE);
    player.setReady(true);

    Team team = new Team();
    team.setColor(TeamColor.BLUE);
    player.setTeam(team);

    PlayerResponseDTO dto = DTOMapper.INSTANCE.convertEntityToPlayerResponseDTO(player);

    assertEquals(10L, dto.getId());
    assertEquals("FIELD_OPERATIVE", dto.getRole());
    assertEquals("BLUE", dto.getTeamColor());
    assertTrue(dto.getReady());
  }

  @Test
  public void testConvertEntityToLobbyDTO_success() {
    Lobby lobby = new Lobby();
    lobby.setId(123L);
    lobby.setLobbyName("WebSocket Mapped Lobby");
    lobby.setGameMode(GameMode.CLASSIC);

    LobbyDTO dto = DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby);

    assertEquals(123L, dto.id);
    assertEquals("WebSocket Mapped Lobby", dto.lobbyName);
    assertEquals("CLASSIC", dto.gameMode);
  }

  @Test
    public void testRoleUpdateDTO_usageSimulation() {
        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setRole("SPYMASTER");

        assertEquals("SPYMASTER", dto.getRole());
    }

}

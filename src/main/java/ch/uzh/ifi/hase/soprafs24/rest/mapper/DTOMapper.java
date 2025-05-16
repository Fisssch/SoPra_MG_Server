package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GetLobbyDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.PlayerUpdateDTO;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DTOMapper {

    DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "token", ignore = true)
    @Mapping(target = "onlineStatus", ignore = true)
    @Mapping(target = "losses", ignore = true)
    @Mapping(target = "blackCardGuesses", ignore = true)
    @Mapping(target = "ready", ignore = true)
    @Mapping(source = "username", target = "username")
    @Mapping(source = "password", target = "password")
    User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "onlineStatus", target = "onlineStatus")
    UserGetDTO convertEntityToUserGetDTO(User user);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "lobbyName", target = "lobbyName")
    @Mapping(source = "gameMode", target = "gameMode")
    @Mapping(source = "lobbyCode", target = "lobbyCode")
    @Mapping(source = "openForLostPlayers", target = "openForLostPlayers")
    GetLobbyDTO convertEntitytoGetLobbyDTO(Lobby lobby);

    LobbyDTO convertEntityToLobbyDTO(Lobby lobby);

    @Mapping(target = "playerId", source = "id")
    @Mapping(target = "ready", source = "ready")    
    PlayerUpdateDTO convertEntityToPlayerUpdateDTO(Player player);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "lobbyName", target = "lobbyName")
    @Mapping(source = "gameMode", target = "gameMode")
    @Mapping(source = "lobbyCode", target = "lobbyCode")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "language", target = "language")
    @Mapping(source = "openForLostPlayers", target = "openForLostPlayers")
    @Mapping(source = "turnDuration", target = "turnDuration")
    LobbyResponseDTO convertEntityToLobbyResponseDTO(Lobby lobby);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "role", target = "role")
    @Mapping(source = "ready", target = "ready")
    @Mapping(source = "team.color", target = "teamColor")
    PlayerResponseDTO convertEntityToPlayerResponseDTO(Player player);
}
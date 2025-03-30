package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GetLobbyDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.LobbyDTO;
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
    User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "onlineStatus", target = "onlineStatus")
    UserGetDTO convertEntityToUserGetDTO(User user);

    GetLobbyDTO convertEntitytoGetLobbyDTO(Lobby lobby);

    LobbyDTO convertEntityToLobbyDTO(Lobby lobby);
}
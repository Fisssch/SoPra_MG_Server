package ch.uzh.ifi.hase.soprafs24.dto;

import ch.uzh.ifi.hase.soprafs24.rest.dto.GetLobbyDTO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GetLobbyDTOTest {

    @Test
    public void testGettersAndSetters() {
        GetLobbyDTO dto = new GetLobbyDTO();

        Long id = 1L;
        String lobbyName = "Test Lobby";
        String gameMode = "CLASSIC";
        Integer lobbyCode = 1234;
        List<String> customWords = Arrays.asList("APPLE", "BANANA", "CHERRY");
        boolean openForLostPlayers = true;

        dto.setId(id);
        dto.setLobbyName(lobbyName);
        dto.setGameMode(gameMode);
        dto.setLobbyCode(lobbyCode);
        dto.setCustomWords(customWords);
        dto.setOpenForLostPlayers(openForLostPlayers);

        assertEquals(id, dto.getId());
        assertEquals(lobbyName, dto.getLobbyName());
        assertEquals(gameMode, dto.getGameMode());
        assertEquals(lobbyCode, dto.getLobbyCode());
        assertEquals(customWords, dto.getCustomWords());
        assertTrue(dto.isOpenForLostPlayers());
    }
}
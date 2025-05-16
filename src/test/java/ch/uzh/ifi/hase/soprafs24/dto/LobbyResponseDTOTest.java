package ch.uzh.ifi.hase.soprafs24.dto;

import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class LobbyResponseDTOTest {

    @Test
    public void testConstructorAndGetters() {
        Long id = 1L;
        String lobbyName = "Test";
        String gameMode = "CLASSIC";
        Integer lobbyCode = 1234;
        Instant createdAt = Instant.now();
        String language = "GERMAN";
        boolean open = true;
        Integer turnDuration = 60; 

        LobbyResponseDTO dto = new LobbyResponseDTO(id, lobbyName, gameMode, lobbyCode, createdAt, language, open, turnDuration);
        dto.setLanguage(language); // since constructor param is not used

        assertEquals(id, dto.getId());
        assertEquals(lobbyName, dto.getLobbyName());
        assertEquals(gameMode, dto.getGameMode());
        assertEquals(lobbyCode, dto.getLobbyCode());
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(language, dto.getLanguage());
        assertTrue(dto.isOpenForLostPlayers());
    }

    @Test
    public void testSetters() {
        LobbyResponseDTO dto = new LobbyResponseDTO(null, null, null, null, null, null, false, null);

        Instant time = Instant.now();
        dto.setId(2L);
        dto.setLobbyName("New Lobby");
        dto.setGameMode("THEME");
        dto.setLobbyCode(9999);
        dto.setCreatedAt(time);
        dto.setLanguage("ENGLISH");
        dto.setOpenForLostPlayers(true);

        assertEquals(2L, dto.getId());
        assertEquals("New Lobby", dto.getLobbyName());
        assertEquals("THEME", dto.getGameMode());
        assertEquals(9999, dto.getLobbyCode());
        assertEquals(time, dto.getCreatedAt());
        assertEquals("ENGLISH", dto.getLanguage());
        assertTrue(dto.isOpenForLostPlayers());
    }
}
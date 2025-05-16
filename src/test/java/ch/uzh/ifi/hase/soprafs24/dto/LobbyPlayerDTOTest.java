package ch.uzh.ifi.hase.soprafs24.dto;

import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyPlayerDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LobbyPlayerDTOTest {

    @Test
    public void testConstructorAndGetters() {
        LobbyPlayerDTO dto = new LobbyPlayerDTO("user", "SPYMASTER", "RED", true);

        assertEquals("user", dto.getUsername());
        assertEquals("SPYMASTER", dto.getRole());
        assertEquals("RED", dto.getTeam());
        assertTrue(dto.isReady());
    }

    @Test
    public void testSetters() {
        LobbyPlayerDTO dto = new LobbyPlayerDTO(null, null, null, false);

        dto.setUsername("newUser");
        dto.setRole("FIELD_OPERATIVE");
        dto.setTeam("BLUE");
        dto.setReady(true);

        assertEquals("newUser", dto.getUsername());
        assertEquals("FIELD_OPERATIVE", dto.getRole());
        assertEquals("BLUE", dto.getTeam());
        assertTrue(dto.isReady());
    }
}
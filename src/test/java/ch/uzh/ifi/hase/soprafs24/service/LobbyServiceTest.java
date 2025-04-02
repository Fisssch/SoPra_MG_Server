package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.*;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LobbyServiceTest {

    private LobbyService lobbyService;
    private LobbyRepository lobbyRepository;
    private PlayerRepository playerRepository;
    private WebsocketService websocketService;
    private TeamRepository teamRepository;

    @BeforeEach
    public void setup() {
        lobbyRepository = Mockito.mock(LobbyRepository.class);
        playerRepository = Mockito.mock(PlayerRepository.class);
        websocketService = Mockito.mock(WebsocketService.class);
        teamRepository = Mockito.mock(TeamRepository.class);
        lobbyService = new LobbyService(lobbyRepository, playerRepository, teamRepository);

        // Wenn save() aufgerufen wird, gib die Lobby direkt zurück
        when(lobbyRepository.save(any(Lobby.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void testTeamBalanceForDifferentPlayerCounts() {
        int[] playerCountsToTest = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}; // beliebig erweiterbar

        for (int count : playerCountsToTest) {
            // Neue Lobby für jeden Durchlauf
            Lobby lobby = lobbyService.createLobby("BalanceTest-" + count, GameMode.CLASSIC);
            when(lobbyRepository.findById(any())).thenReturn(Optional.of(lobby));

            // Spieler hinzufügen
            for (long i = 1; i <= count; i++) {
                lobbyService.addPlayerToLobby(1L, i);
            }

            // Spieler pro Team zählen
            long redCount = lobby.getPlayers().stream()
                    .filter(p -> "red".equals(p.getTeam().getColor()))
                    .count();

            long blueCount = lobby.getPlayers().stream()
                    .filter(p -> "blue".equals(p.getTeam().getColor()))
                    .count();

            long diff = Math.abs(redCount - blueCount);

            // Assert
            assertTrue(diff <= 1, String.format("Fehler bei %d Spielern – red: %d, blue: %d", count, redCount, blueCount));
        }
    }
    @Test
    public void testEachTeamHasOnlyOneSpymaster() {
        // Given
        Lobby lobby = lobbyService.createLobby("TestLobby", GameMode.OWN_WORDS);
        when(lobbyRepository.findById(any())).thenReturn(Optional.of(lobby));

        // Spieler der Lobby hinzufügen (Service übernimmt Team- und Rollenlogik)
        lobbyService.addPlayerToLobby(1L, 1L);
        lobbyService.addPlayerToLobby(1L, 2L);
        lobbyService.addPlayerToLobby(1L, 3L);
        lobbyService.addPlayerToLobby(1L, 4L);

        // Spymaster-Zählung pro Team
        long redSpymasters = lobby.getPlayers().stream()
                .filter(p -> p.getTeam().getColor().equals(TeamColor.RED))
                .filter(p -> PlayerRole.SPYMASTER.equals(p.getRole()))
                .count();

        long blueSpymasters = lobby.getPlayers().stream()
                .filter(p -> p.getTeam().getColor().equals(TeamColor.BLUE))
                .filter(p -> PlayerRole.SPYMASTER.equals(p.getRole()))
                .count();

        assertEquals(1, redSpymasters, "Red team should have exactly one spymaster");
        assertEquals(1, blueSpymasters, "Blue team should have exactly one spymaster");
    }
    @Test
    public void shouldStartGame_returnsTrue_whenAllPlayersReadyAndAtLeastFour() {
        Lobby lobby = new Lobby();

        for (int i = 0; i < 4; i++) {
            Player p = new Player();
            p.setReady(true);
            lobby.addPlayer(p);
        }

        boolean result = lobbyService.shouldStartGame(lobby);
        assertTrue(result, "Game should start when 4+ players are all ready");
    }

    @Test
    public void shouldStartGame_returnsFalse_whenNotAllPlayersReady() {
        Lobby lobby = new Lobby();

        for (int i = 0; i < 4; i++) {
            Player p = new Player();
            p.setReady(i < 3); // letzter Spieler ist nicht bereit
            lobby.addPlayer(p);
        }

        boolean result = lobbyService.shouldStartGame(lobby);
        assertFalse(result, "Game should not start if not all players are ready");
    }

    @Test
    public void shouldStartGame_returnsFalse_whenLessThanFourPlayers() {
        Lobby lobby = new Lobby();

        for (int i = 0; i < 3; i++) {
            Player p = new Player();
            p.setReady(true);
            lobby.addPlayer(p);
        }

        boolean result = lobbyService.shouldStartGame(lobby);
        assertFalse(result, "Game should not start with less than 4 players");
    }

    @Test
    public void setPlayerReadyStatus_notFound_throwsError() {
        assertThrows(ResponseStatusException.class, () -> lobbyService.setPlayerReadyStatus(1L, 1L, true, websocketService));
    }

    @Test
    public void changePlayerRole_invalidRole_throwsError() {
        assertThrows(ResponseStatusException.class, () -> lobbyService.changePlayerRole(1L, 1L, "hacker"));
    }

    @Test
    public void changePlayerTeam_invalidColor_returns400() throws Exception {
        assertThrows(ResponseStatusException.class, () -> lobbyService.changePlayerTeam(1L, 1L, "green"));
    }
}
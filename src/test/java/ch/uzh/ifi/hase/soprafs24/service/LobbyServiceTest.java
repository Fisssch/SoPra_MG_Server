package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LobbyServiceTest {

    private LobbyService lobbyService;
    private LobbyRepository lobbyRepository;

    @BeforeEach
    public void setup() {
        lobbyRepository = Mockito.mock(LobbyRepository.class);
        lobbyService = new LobbyService(lobbyRepository);

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
                Player player = new Player();
                player.setId(i);
                lobbyService.addPlayerToLobby(1L, player);
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

        // Vier Spieler erzeugen
        Player player1 = new Player(); player1.setId(1L);
        Player player2 = new Player(); player2.setId(2L);
        Player player3 = new Player(); player3.setId(3L);
        Player player4 = new Player(); player4.setId(4L);

        // Spieler der Lobby hinzufügen (Service übernimmt Team- und Rollenlogik)
        lobbyService.addPlayerToLobby(1L, player1);
        lobbyService.addPlayerToLobby(1L, player2);
        lobbyService.addPlayerToLobby(1L, player3);
        lobbyService.addPlayerToLobby(1L, player4);

        // Spymaster-Zählung pro Team
        long redSpymasters = lobby.getPlayers().stream()
                .filter(p -> p.getTeam().getColor().equals("red"))
                .filter(p -> "spymaster".equals(p.getRole()))
                .count();

        long blueSpymasters = lobby.getPlayers().stream()
                .filter(p -> p.getTeam().getColor().equals("blue"))
                .filter(p -> "spymaster".equals(p.getRole()))
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
}
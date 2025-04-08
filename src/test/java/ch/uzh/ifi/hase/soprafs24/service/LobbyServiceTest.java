package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.*;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

        when(lobbyRepository.save(any(Lobby.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    class TeamAndRoleLogic {

        @Test
        public void teamIsBalanced_whenMultiplePlayersJoin() {
            int[] playerCounts = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

            for (int count : playerCounts) {
                Lobby lobby = lobbyService.createLobby("Lobby-" + count, GameMode.CLASSIC);
                when(lobbyRepository.findById(any())).thenReturn(Optional.of(lobby));

                for (long i = 1; i <= count; i++) {
                    lobbyService.addPlayerToLobby(1L, i);
                }

                long red = lobby.getPlayers().stream().filter(p -> "red".equals(p.getTeam().getColor())).count();
                long blue = lobby.getPlayers().stream().filter(p -> "blue".equals(p.getTeam().getColor())).count();

                assertTrue(Math.abs(red - blue) <= 1, "Unbalanced teams at " + count + " players");
            }
        }

        @Test
        public void eachTeamHasOnlyOneSpymaster() {
            Lobby lobby = lobbyService.createLobby("SpymasterTest", GameMode.OWN_WORDS);
            when(lobbyRepository.findById(any())).thenReturn(Optional.of(lobby));

            for (long i = 1; i <= 4; i++) {
                lobbyService.addPlayerToLobby(1L, i);
            }

            long redSpymasters = lobby.getPlayers().stream()
                    .filter(p -> TeamColor.RED.equals(p.getTeam().getColor()))
                    .filter(p -> PlayerRole.SPYMASTER.equals(p.getRole()))
                    .count();

            long blueSpymasters = lobby.getPlayers().stream()
                    .filter(p -> TeamColor.BLUE.equals(p.getTeam().getColor()))
                    .filter(p -> PlayerRole.SPYMASTER.equals(p.getRole()))
                    .count();

            assertEquals(1, redSpymasters);
            assertEquals(1, blueSpymasters);
        }

        @Test
        public void changePlayerTeam_invalidColor_throwsError() {
            assertThrows(ResponseStatusException.class,
                    () -> lobbyService.changePlayerTeam(1L, 1L, "green"));
        }

        @Test
        public void changePlayerRole_invalidRole_throwsError() {
            assertThrows(ResponseStatusException.class,
                    () -> lobbyService.changePlayerRole(1L, 1L, "hacker"));
        }
    }

    @Nested
    class GameStartLogic {

        @Test
        public void gameStarts_whenAllReady_andAtLeastFour() {
            Lobby lobby = new Lobby();
            for (int i = 0; i < 4; i++) {
                Player p = new Player();
                p.setReady(true);
                lobby.addPlayer(p);
            }

            assertTrue(lobbyService.shouldStartGame(lobby));
        }

        @Test
        public void gameDoesNotStart_whenNotAllReady() {
            Lobby lobby = new Lobby();
            for (int i = 0; i < 4; i++) {
                Player p = new Player();
                p.setReady(i < 3); // last one is not ready
                lobby.addPlayer(p);
            }

            assertFalse(lobbyService.shouldStartGame(lobby));
        }

        @Test
        public void gameDoesNotStart_whenLessThanFourPlayers() {
            Lobby lobby = new Lobby();
            for (int i = 0; i < 3; i++) {
                Player p = new Player();
                p.setReady(true);
                lobby.addPlayer(p);
            }

            assertFalse(lobbyService.shouldStartGame(lobby));
        }
    }

    @Nested
    class PlayerReadyStatus {

        @Test
        public void setPlayerReadyStatus_notFound_throwsError() {
            assertThrows(ResponseStatusException.class,
                    () -> lobbyService.setPlayerReadyStatus(1L, 1L, true, websocketService));
        }
    }

    @Nested
    class PlayerRemoval {

        @Test
        public void removePlayerFromLobby_deletesLobby_whenLastPlayerLeaves() {
            Player player = new Player();
            player.setId(1L);
            Lobby lobby = new Lobby();
            lobby.setId(42L);
            lobby.addPlayer(player);

            when(lobbyRepository.findById(42L)).thenReturn(Optional.of(lobby));
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

            lobbyService.removePlayerFromLobby(42L, 1L);

            verify(playerRepository).delete(player);
            verify(lobbyRepository).delete(lobby);
        }

        @Test
        public void removePlayerFromLobby_keepsLobby_whenOthersRemain() {
            Player p1 = new Player(); p1.setId(1L);
            Player p2 = new Player(); p2.setId(2L);

            Lobby lobby = new Lobby();
            lobby.setId(43L);
            lobby.addPlayer(p1);
            lobby.addPlayer(p2);

            when(lobbyRepository.findById(43L)).thenReturn(Optional.of(lobby));
            when(playerRepository.findById(1L)).thenReturn(Optional.of(p1));

            lobbyService.removePlayerFromLobby(43L, 1L);

            verify(playerRepository).delete(p1);
            verify(lobbyRepository).save(lobby);
            verify(lobbyRepository, never()).delete(any());
        }
    }
}
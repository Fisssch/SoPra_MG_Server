package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.*;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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
        lobbyService = new LobbyService(lobbyRepository, playerRepository, teamRepository, websocketService);

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
        
        @Test
        public void changePlayerRole_toSpymaster_success() {
            // Setup
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            
            Player player = new Player();
            player.setId(1L);
            player.setRole(PlayerRole.FIELD_OPERATIVE);
            
            Team team = new Team();
            team.setColor(TeamColor.RED);
            team.setLobby(lobby);
            player.setTeam(team);
            
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
            
            // Act
            Player result = lobbyService.changePlayerRole(1L, 1L, "SPYMASTER");
            
            // Assert
            assertEquals(PlayerRole.SPYMASTER, result.getRole());
            verify(playerRepository).save(player);
            verify(teamRepository).save(team);
        }
        
        @Test
        public void changePlayerRole_whenTeamAlreadyHasSpymaster_throwsConflict() {
            // Setup
            Player existingSpymaster = new Player();
            existingSpymaster.setId(2L);
            existingSpymaster.setRole(PlayerRole.SPYMASTER);
            
            Team team = new Team();
            team.setColor(TeamColor.RED);
            team.setSpymaster(existingSpymaster);
            
            Player player = new Player();
            player.setId(1L);
            player.setRole(PlayerRole.FIELD_OPERATIVE);
            player.setTeam(team);
            
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
            
            // Act & Assert
            assertThrows(ResponseStatusException.class, 
                    () -> lobbyService.changePlayerRole(1L, 1L, "SPYMASTER"));
        }

        @Test
        public void changePlayerTeam_success() {
            // Setup
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            
            Team redTeam = new Team();
            redTeam.setColor(TeamColor.RED);
            
            Team blueTeam = new Team();
            blueTeam.setColor(TeamColor.BLUE);
            
            lobby.setRedTeam(redTeam);
            lobby.setBlueTeam(blueTeam);
            
            Player player = new Player();
            player.setId(1L);
            player.setRole(PlayerRole.FIELD_OPERATIVE);
            player.setTeam(redTeam);
            
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
            
            // Act
            Player result = lobbyService.changePlayerTeam(1L, 1L, "blue");
            
            // Assert
            assertEquals(blueTeam, result.getTeam());
            verify(lobbyRepository).save(lobby);
            verify(playerRepository).save(player);
        }
    }

    @Nested
    class GameStartLogic {

        @Test
        public void gameStarts_whenAllReady_andAtLeastFour() {
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            lobby.setPlayers(new ArrayList<>());

            Team redTeam = new Team();
            redTeam.setId(101L);
            redTeam.setColor(TeamColor.RED);
            redTeam.setPlayers(new ArrayList<>());

            Team blueTeam = new Team();
            blueTeam.setId(102L);
            blueTeam.setColor(TeamColor.BLUE);
            blueTeam.setPlayers(new ArrayList<>());

            lobby.setRedTeam(redTeam);
            lobby.setBlueTeam(blueTeam);

            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            when(playerRepository.findById(anyLong())).thenAnswer(invocation -> {
                Long id = invocation.getArgument(0);
                Player p = new Player();
                p.setId(id);
                return Optional.of(p);
            });

            // Add Spymasters
            Player redSpymaster = lobbyService.addPlayerToLobby(1L, 1L);
            redSpymaster.setReady(true);
            redSpymaster.setRole(PlayerRole.SPYMASTER);
            redTeam.setSpymaster(redSpymaster);

            Player blueSpymaster = lobbyService.addPlayerToLobby(1L, 2L);
            blueSpymaster.setReady(true);
            blueSpymaster.setRole(PlayerRole.SPYMASTER);
            blueTeam.setSpymaster(blueSpymaster);

            // Add 3 ready field operatives to each team
            for (long i = 3L; i <= 8L; i++) {
                Player p = lobbyService.addPlayerToLobby(1L, i);
                p.setReady(true);
            }

            assertTrue(lobbyService.shouldStartGame(lobby));
        }

        @Test
        public void gameDoesNotStart_whenNotAllReady() {
            Lobby lobby = new Lobby();
            lobby.setId(2L);
            lobby.setPlayers(new ArrayList<>());

            Team redTeam = new Team();
            redTeam.setId(103L);
            redTeam.setColor(TeamColor.RED);
            redTeam.setPlayers(new ArrayList<>());

            Team blueTeam = new Team();
            blueTeam.setId(104L);
            blueTeam.setColor(TeamColor.BLUE);
            blueTeam.setPlayers(new ArrayList<>());

            lobby.setRedTeam(redTeam);
            lobby.setBlueTeam(blueTeam);

            when(lobbyRepository.findById(2L)).thenReturn(Optional.of(lobby));
            when(playerRepository.findById(anyLong())).thenAnswer(invocation -> {
                Long id = invocation.getArgument(0);
                Player p = new Player();
                p.setId(id);
                return Optional.of(p);
            });

            // Add Spymasters
            Player redSpymaster = lobbyService.addPlayerToLobby(2L, 10L);
            redSpymaster.setReady(true);
            redSpymaster.setRole(PlayerRole.SPYMASTER);
            redTeam.setSpymaster(redSpymaster);

            Player blueSpymaster = lobbyService.addPlayerToLobby(2L, 20L);
            blueSpymaster.setReady(true);
            blueSpymaster.setRole(PlayerRole.SPYMASTER);
            blueTeam.setSpymaster(blueSpymaster);

            // Add 3 operatives each team (one operative not ready)
            for (long i = 30L; i <= 35L; i++) {
                Player p = lobbyService.addPlayerToLobby(2L, i);
                p.setReady(i != 35L); // last player not ready
            }

            assertFalse(lobbyService.shouldStartGame(lobby));
        }
        
        @Test
        public void gameDoesNotStart_whenNoSpymasters() {
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            lobby.setPlayers(new ArrayList<>());

            Team redTeam = new Team();
            redTeam.setId(100L);
            redTeam.setColor(TeamColor.RED);
            redTeam.setPlayers(new ArrayList<>());

            Team blueTeam = new Team();
            blueTeam.setId(200L);
            blueTeam.setColor(TeamColor.BLUE);
            blueTeam.setPlayers(new ArrayList<>());

            lobby.setRedTeam(redTeam);
            lobby.setBlueTeam(blueTeam);

            for (int i = 0; i < 6; i++) {
                Player p = new Player();
                p.setId((long) (i + 1)); 
                p.setReady(true);
                p.setRole(PlayerRole.FIELD_OPERATIVE);
                if (i < 3) {
                    lobby.assignPlayerToTeam(p, redTeam);
                } else {
                    lobby.assignPlayerToTeam(p, blueTeam);
                }
            }
            assertFalse(lobbyService.shouldStartGame(lobby));
        }
    }

    @Nested
    class PlayerReadyStatus {

        @Test
        public void setPlayerReadyStatus_notFound_throwsError() {
            when(playerRepository.findById(anyLong())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
            
            assertThrows(ResponseStatusException.class,
                    () -> lobbyService.setPlayerReadyStatus(1L, 1L, true, websocketService));
        }
        
        @Test
        public void setPlayerReadyStatus_success() {
            // Setup
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            
            Player player = new Player();
            player.setId(1L);
            player.setReady(false);
            
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
            
            // Act
            Player result = lobbyService.setPlayerReadyStatus(1L, 1L, true, websocketService);
            
            // Assert
            assertTrue(result.getReady());
            verify(playerRepository).save(player);
        }
        
        @Test
        public void getPlayerReadyStatus_success() {
            // Setup
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            
            Player player = new Player();
            player.setId(1L);
            player.setReady(true);
            
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
            
            // Act
            Boolean result = lobbyService.getPlayerReadyStatus(1L, 1L);
            
            // Assert
            assertTrue(result);
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
        
        @Test
        public void removeSpymasterFromLobby_clearsTeamSpymaster() {
            // Setup
            Player player = new Player();
            player.setId(1L);
            player.setRole(PlayerRole.SPYMASTER);
            
            Team team = new Team();
            team.setSpymaster(player);
            player.setTeam(team);
            
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            lobby.addPlayer(player);
            Player otherPlayer = new Player();
            otherPlayer.setId(2L);
            lobby.addPlayer(otherPlayer);
            
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
            
            // Act
            lobbyService.removePlayerFromLobby(1L, 1L);
            
            // Assert
            assertNull(team.getSpymaster());
            verify(teamRepository).save(team);
        }
    }
    
    @Nested
    class LobbyManagement {
        
        @Test
        public void createLobby_success() {
            // Act
            Lobby result = lobbyService.createLobby("Test Lobby", GameMode.CLASSIC);
            
            // Assert
            assertEquals("Test Lobby", result.getLobbyName());
            assertEquals(GameMode.CLASSIC, result.getGameMode());
            assertNotNull(result.getRedTeam());
            assertNotNull(result.getBlueTeam());
            assertEquals(TeamColor.RED, result.getRedTeam().getColor());
            assertEquals(TeamColor.BLUE, result.getBlueTeam().getColor());
            verify(lobbyRepository, times(2)).save(any(Lobby.class));
            verify(teamRepository, times(2)).save(any(Team.class));
        }
        
        @Test
        public void getLobbyById_notFound_throwsException() {
            // Setup
            when(lobbyRepository.findById(anyLong())).thenReturn(Optional.empty());
            
            // Act & Assert
            assertThrows(ResponseStatusException.class, () -> lobbyService.getLobbyById(1L));
        }
        
        @Test
        public void getLobbyById_success() {
            // Setup
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            
            // Act
            Lobby result = lobbyService.getLobbyById(1L);
            
            // Assert
            assertEquals(1L, result.getId());
        }
        
        @Test
        public void getOrCreateLobby_codeProvided_lobbyFound() {
            // Setup
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            lobby.setLobbyCode(1234);
            when(lobbyRepository.findByLobbyCode(1234)).thenReturn(Optional.of(lobby));
            
            // Act
            Lobby result = lobbyService.getOrCreateLobby(1234);
            
            // Assert
            assertEquals(1L, result.getId());
            assertEquals(1234, result.getLobbyCode());
        }
        
        @Test
        public void getOrCreateLobby_codeProvided_notFound_throwsException() {
            // Setup
            when(lobbyRepository.findByLobbyCode(anyInt())).thenReturn(Optional.empty());
            
            // Act & Assert
            assertThrows(ResponseStatusException.class, () -> lobbyService.getOrCreateLobby(1234));
        }
        
        @Test
        public void setGameMode_success() {
            // Setup
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            lobby.setGameMode(GameMode.CLASSIC);
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            
            // Act
            Lobby result = lobbyService.setGameMode(1L, GameMode.OWN_WORDS);
            
            // Assert
            assertEquals(GameMode.OWN_WORDS, result.getGameMode());
            verify(lobbyRepository).save(lobby);
        }
    }
    
    @Nested
    class CustomWordManagement {
        
        @Test
        public void addCustomWord_success() {
            // Setup
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            lobby.setGameMode(GameMode.OWN_WORDS);
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            
            // Act
            Lobby result = lobbyService.addCustomWord(1L, "test");
            
            // Assert
            assertEquals(1, result.getCustomWords().size());
            assertEquals("TEST", result.getCustomWords().get(0));
            verify(lobbyRepository).save(lobby);
        }
        
        @Test
        public void addCustomWord_duplicateWord_notAdded() {
            // Setup
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            lobby.setGameMode(GameMode.OWN_WORDS);
            List<String> words = new ArrayList<>();
            words.add("TEST");
            lobby.setCustomWords(words);
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            
            // Act
            Lobby result = lobbyService.addCustomWord(1L, "test");
            
            // Assert
            assertEquals(1, result.getCustomWords().size());
            verify(lobbyRepository).save(lobby);
        }
        
        @Test
        public void addCustomWord_emptyWord_throwsException() {
            // Setup
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            
            // Act & Assert
            assertThrows(ResponseStatusException.class, () -> lobbyService.addCustomWord(1L, ""));
        }
        
        @Test
        public void addCustomWord_nullWord_throwsException() {
            // Setup
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            
            // Act & Assert
            assertThrows(ResponseStatusException.class, () -> lobbyService.addCustomWord(1L, null));
        }
        
        @Test
        public void addCustomWord_wordWithSpace_throwsException() {
            // Setup
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            
            // Act & Assert
            assertThrows(ResponseStatusException.class, () -> lobbyService.addCustomWord(1L, "test word"));
        }
        
        @Test
        public void addCustomWord_tooManyWords_throwsException() {
            // Setup
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            List<String> words = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                words.add("WORD" + i);
            }
            lobby.setCustomWords(words);
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            
            // Act & Assert
            assertThrows(ResponseStatusException.class, () -> lobbyService.addCustomWord(1L, "toomany"));
        }
    }
}
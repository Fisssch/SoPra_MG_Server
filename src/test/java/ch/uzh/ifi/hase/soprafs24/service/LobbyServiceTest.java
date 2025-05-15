package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.*;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.*;

import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyPlayersResponseDTO;
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
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        lobbyRepository = Mockito.mock(LobbyRepository.class);
        playerRepository = Mockito.mock(PlayerRepository.class);
        websocketService = Mockito.mock(WebsocketService.class);
        teamRepository = Mockito.mock(TeamRepository.class);
        userRepository = Mockito.mock(UserRepository.class);

        lobbyService = new LobbyService(lobbyRepository, playerRepository, teamRepository, websocketService, userRepository);

        when(lobbyRepository.save(any(Lobby.class)))
                .thenAnswer(invocation -> {
                    Lobby lobby = invocation.getArgument(0);
                    if (lobby.getId() == null) {
                        lobby.setId(1L); // oder ein Zähler für mehrere Lobbys
                    }
                    return lobby;
                });

        when(teamRepository.save(any(Team.class)))
                .thenAnswer(invocation -> {
                    Team team = invocation.getArgument(0);
                    if (team.getId() == null) {
                        team.setId((long) (Math.random() * 1000)); // zufällige ID oder auch fixiert
                    }
                    return team;
                });
    }

    @Nested
    class TeamAndRoleLogic {

        @Test
        public void teamIsBalanced_whenMultiplePlayersJoin() {
            int[] playerCounts = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

            for (int count : playerCounts) {
                Lobby lobby = lobbyService.createLobby("Lobby-" + count, GameMode.CLASSIC, false);
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
            Lobby lobby = lobbyService.createLobby("SpymasterTest", GameMode.OWN_WORDS, false);
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
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            
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

        @Test
        public void changePlayerTeam_spymasterChangesRoleIfTargetTeamHasSpymaster() {
            Lobby lobby = new Lobby();
            lobby.setId(1L);

            Team redTeam = new Team();
            redTeam.setColor(TeamColor.RED);

            Team blueTeam = new Team();
            blueTeam.setColor(TeamColor.BLUE);

            Player existingSpymaster = new Player();
            existingSpymaster.setId(2L);
            existingSpymaster.setRole(PlayerRole.SPYMASTER);
            blueTeam.setSpymaster(existingSpymaster);

            Player player = new Player();
            player.setId(1L);
            player.setRole(PlayerRole.SPYMASTER);
            player.setTeam(redTeam);

            lobby.setRedTeam(redTeam);
            lobby.setBlueTeam(blueTeam);

            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

            Player result = lobbyService.changePlayerTeam(1L, 1L, "blue");

            assertEquals(blueTeam, result.getTeam());
            assertEquals(PlayerRole.FIELD_OPERATIVE, result.getRole());
            verify(playerRepository).save(player);
        }

        @Test
        public void changePlayerRole_fromSpymasterToFieldOperative_updatesTeamSpymaster() {
            Player player = new Player();
            player.setId(1L);
            player.setRole(PlayerRole.SPYMASTER);

            Team team = new Team();
            team.setColor(TeamColor.RED);
            team.setSpymaster(player);
            player.setTeam(team);

            Lobby lobby = new Lobby();
            lobby.setId(1L);

            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

            Player result = lobbyService.changePlayerRole(1L, 1L, "field_operative");

            assertEquals(PlayerRole.FIELD_OPERATIVE, result.getRole());
            assertNull(team.getSpymaster());
            verify(teamRepository).save(team);
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
            Lobby result = lobbyService.createLobby("Test Lobby", GameMode.CLASSIC, false);
            
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
            Lobby result = lobbyService.getOrCreateLobby(1234, false);
            
            // Assert
            assertEquals(1L, result.getId());
            assertEquals(1234, result.getLobbyCode());
        }
        
        @Test
        public void getOrCreateLobby_codeProvided_notFound_throwsException() {
            // Setup
            when(lobbyRepository.findByLobbyCode(anyInt())).thenReturn(Optional.empty());
            
            // Act & Assert
            assertThrows(ResponseStatusException.class, () -> lobbyService.getOrCreateLobby(1234,false));
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

    @Test
    public void removeCustomWord_success() {
        Lobby lobby = new Lobby();
        lobby.setId(1L);
        lobby.setGameMode(GameMode.OWN_WORDS);
        lobby.setCustomWords(new ArrayList<>(List.of("TEST")));
        when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

        Lobby result = lobbyService.removeCustomWord(1L, "test");

        assertTrue(result.getCustomWords().isEmpty());
        verify(lobbyRepository).save(lobby);
    }
    @Nested
    class MiscellaneousServiceTests {

        @Test
        public void setOpenForLostPlayers_setsFlagTrue() {
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            lobby.setOpenForLostPlayers(false);

            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

            lobbyService.setOpenForLostPlayers(1L, true);

            assertTrue(lobby.isOpenForLostPlayers());
            verify(lobbyRepository).save(lobby);
        }

        @Test
        public void getAllJoinableLobbies_returnsOnlyOpenAndUnstartedLobbies() {
            Lobby openLobby = new Lobby();
            openLobby.setId(1L);
            openLobby.setGameStarted(false);

            Lobby startedLobby = new Lobby();
            startedLobby.setId(2L);
            startedLobby.setGameStarted(true);

            when(lobbyRepository.findOpenLobbiesForLostPlayers()).thenReturn(List.of(openLobby, startedLobby));

            List<Lobby> result = lobbyService.getAllJoinableLobbies();

            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getId());
        }

        @Test
        public void getTeamColorByPlayer_returnsCorrectColor() {
            Player player = new Player();
            Team team = new Team();
            team.setColor(TeamColor.BLUE);
            player.setTeam(team);

            when(playerRepository.findById(42L)).thenReturn(Optional.of(player));

            TeamColor color = lobbyService.getTeamColorByPlayer(42L);

            assertEquals(TeamColor.BLUE, color);
        }

        @Test
        public void setTheme_setsThemeTrimmed() {
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

            Lobby result = lobbyService.setTheme(1L, " space-theme ");
            assertEquals("space-theme", result.getTheme());
            verify(lobbyRepository).save(lobby);
        }

        @Test
        public void setLanguage_setsLanguage() {
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

            Lobby result = lobbyService.setLanguage(1L, GameLanguage.FRENCH);
            assertEquals(GameLanguage.FRENCH, result.getLanguage());
            verify(lobbyRepository).save(lobby);
        }

        @Test
        public void sendLobbyPlayerStatusUpdate_sendsCorrectDTO() {
            Lobby lobby = new Lobby();
            lobby.setId(1L);

            // Spieler 1
            Player player1 = new Player();
            player1.setId(1L);
            player1.setRole(PlayerRole.FIELD_OPERATIVE);
            Team redTeam = new Team();
            redTeam.setColor(TeamColor.RED);
            player1.setTeam(redTeam);
            player1.setReady(true);

            // Spieler 2
            Player player2 = new Player();
            player2.setId(2L);
            player2.setRole(PlayerRole.SPYMASTER);
            Team blueTeam = new Team();
            blueTeam.setColor(TeamColor.BLUE);
            player2.setTeam(blueTeam);
            player2.setReady(true);

            lobby.addPlayer(player1);
            lobby.addPlayer(player2);

            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

            User user1 = new User(); user1.setUsername("user1");
            User user2 = new User(); user2.setUsername("user2");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

            LobbyPlayersResponseDTO dto = lobbyService.sendLobbyPlayerStatusUpdate(1L);

            assertEquals(2, dto.getTotalPlayers());
            assertEquals(2, dto.getReadyPlayers());
            assertEquals("user1", dto.getPlayers().get(0).getUsername());
            assertEquals("user2", dto.getPlayers().get(1).getUsername());
        }

        @Test
        public void closeLobby_deletesLobbyPlayersTeamsAndSendsWebsocketMessage() {
            Lobby lobby = new Lobby();
            lobby.setId(99L);

            Player p1 = new Player(); p1.setId(1L);
            Player p2 = new Player(); p2.setId(2L);
            lobby.setPlayers(new ArrayList<>(List.of(p1, p2)));

            Team redTeam = new Team(); redTeam.setId(10L);
            Team blueTeam = new Team(); blueTeam.setId(20L);
            lobby.setRedTeam(redTeam);
            lobby.setBlueTeam(blueTeam);

            when(lobbyRepository.findById(99L)).thenReturn(Optional.of(lobby));
            when(playerRepository.existsById(1L)).thenReturn(true);
            when(playerRepository.existsById(2L)).thenReturn(true);
            when(teamRepository.existsById(10L)).thenReturn(true);
            when(teamRepository.existsById(20L)).thenReturn(true);
            when(lobbyRepository.existsById(99L)).thenReturn(true);

            lobbyService.closeLobby(99L);

            verify(playerRepository).deleteById(1L);
            verify(playerRepository).deleteById(2L);
            verify(teamRepository).deleteById(10L);
            verify(teamRepository).deleteById(20L);
            verify(lobbyRepository).deleteById(99L);
            verify(websocketService).sendMessage("/topic/lobby/99/close", "CLOSED");
        }
    }
}#
}
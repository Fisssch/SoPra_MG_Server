package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.api.apiToken;
import ch.uzh.ifi.hase.soprafs24.constant.CardColor;
import ch.uzh.ifi.hase.soprafs24.constant.GameLanguage;
import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.Team;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class GameServiceTest {

    @Mock
    private WordGenerationService wordGenerationService;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebsocketService websocketService;

    @InjectMocks
    private GameService gameService;

    private Game game;
    private Lobby lobby;

    @BeforeEach
    public void setup() {
        apiToken.isTestEnvironment = true; // Set to true for testing purposes
        MockitoAnnotations.openMocks(this);

        doNothing().when(websocketService).sendMessage(anyString(), any());
        // Setup a dummy Game
        game = new Game();
        game.setId(1L);
        game.setWords(new ArrayList<>());
        game.setBoard(new ArrayList<>());

        // Setup a dummy Lobby
        lobby = new Lobby();
        lobby.setId(1L);
        lobby.setGameMode(GameMode.CLASSIC);
        lobby.setCustomWords(new ArrayList<>());
    }

    //start or get game 
    @Test
    public void startOrGetGame_existingGame_returnsGame() {
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        Game result = gameService.startOrGetGame(1L, TeamColor.RED, GameMode.CLASSIC);

        assertEquals(game.getId(), result.getId());
        verify(gameRepository, never()).save(game); // Should not save because already exists
    }

    @Test
    public void startOrGetGame_newGame_createdSuccessfully() {
        when(gameRepository.findById(1L))
            .thenReturn(Optional.empty()); 

        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game g = invocation.getArgument(0);
            g.setId(1L);
            return g;
        });

        when(wordGenerationService.getWordsFromApi(GameLanguage.GERMAN)).thenReturn(Arrays.asList(
            "apple", "banana", "cherry", "dog", "cat", "tree", "house", "river",
            "car", "mountain", "bird", "school", "computer", "book", "phone",
            "chair", "sun", "moon", "star", "water", "pen", "desk", "cloud",
            "road", "train"
        ));

        Lobby dummyLobby = new Lobby();
        dummyLobby.setId(1L);
        dummyLobby.setCustomWords(new ArrayList<>());
        dummyLobby.setGameMode(GameMode.CLASSIC);
        dummyLobby.setTheme("default");
        dummyLobby.setRedTeam(new Team());
        dummyLobby.setBlueTeam(new Team());
        when(lobbyRepository.findById(1L)).thenReturn(Optional.of(dummyLobby));

        Game result = gameService.startOrGetGame(1L, TeamColor.RED, GameMode.CLASSIC);

        assertNotNull(result);
        assertEquals(25, result.getWords().size());
        assertEquals(25, result.getBoard().size());
        assertEquals(TeamColor.RED, result.getStartingTeam());
        assertEquals("playing", result.getStatus());
    }

    @Test
    public void generateWords_customWords_addedAndFilledTo25() {
        Game game = new Game();
        game.setId(1L);

        when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

        lobby.setCustomWords(Arrays.asList("custom1", "custom2"));
        lobby.setGameMode(GameMode.OWN_WORDS);

        when(wordGenerationService.getWordsFromApi(GameLanguage.GERMAN)).thenReturn(Arrays.asList(
            "word1", "word2", "word3", "word4", "word5", "word6", "word7", "word8",
            "word9", "word10", "word11", "word12", "word13", "word14", "word15", "word16",
            "word17", "word18", "word19", "word20", "word21", "word22", "word23", "word24", "word25"
        ));

        List<String> result = gameService.generateWords(game, "default", GameLanguage.GERMAN);

        assertNotNull(result);
        assertEquals(25, result.size());
        assertTrue(result.contains("CUSTOM1"));
        assertTrue(result.contains("CUSTOM2"));
    }

    @Test
    public void getGameById_gameExists_returnsGame() {
        Game game = new Game();
        game.setId(1L);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        Game result = gameService.getGameById(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    public void getGameById_gameMissing_throwsException() {
        when(gameRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            gameService.getGameById(1L);
        });

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Game not found", ex.getReason());
    }

    @Nested
    class BoardTests {
        @Test
        public void getBoard_success() {
            List<Card> mockCards = new ArrayList<>();
            mockCards.add(new Card("APPLE", CardColor.RED));
            mockCards.add(new Card("BANANA", CardColor.BLUE));
            game.setBoard(mockCards);
            
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            
            List<Card> result = gameService.getBoard(1L);
            
            assertEquals(2, result.size());
            assertEquals("APPLE", result.get(0).getWord());
            assertEquals(CardColor.RED, result.get(0).getColor());
            assertEquals("BANANA", result.get(1).getWord());
            assertEquals(CardColor.BLUE, result.get(1).getColor());
        }
        
        @Test
        public void getBoard_gameNotFound_throwsException() {
            when(gameRepository.findById(99L)).thenReturn(Optional.empty());
            
            assertThrows(ResponseStatusException.class, () -> gameService.getBoard(99L));
        }
    }
    
    @Nested
    class HintTests {
        @Test
        public void checkIfUserSpymaster_success() {
            User user = new User();
            user.setId(1L);
            
            Player player = new Player();
            player.setId(1L);
            player.setRole(PlayerRole.SPYMASTER);
            
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
            
            // Should not throw exception
            gameService.checkIfUserSpymaster(user);
            
            verify(playerRepository).findById(1L);
        }
        
        @Test
        public void checkIfUserSpymaster_notSpymaster_throwsException() {
            User user = new User();
            user.setId(1L);
            
            Player player = new Player();
            player.setId(1L);
            player.setRole(PlayerRole.FIELD_OPERATIVE);
            
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
            
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                    () -> gameService.checkIfUserSpymaster(user));
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
            assertEquals("Only spymasters can give hints", exception.getReason());
        }
        
        @Test
        public void checkIfUserSpymaster_playerNotFound_throwsException() {
            User user = new User();
            user.setId(1L);
            
            when(playerRepository.findById(1L)).thenReturn(Optional.empty());
            
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                    () -> gameService.checkIfUserSpymaster(user));
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertEquals("Player not found", exception.getReason());
        }
        
        @Test
        public void validateHint_validHint_savesHint() {
            Game game = new Game();
            game.setId(1L);
            game.setBoard(new ArrayList<>());
            game.setWords(new ArrayList<>());
            
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            
            gameService.validateHint("forest", 3, 1L);
            
            assertEquals("forest", game.getCurrentHint().getKey());
            assertEquals(3, game.getCurrentHint().getValue());
            verify(gameRepository).save(game);
        }
        
        @Test
        public void validateHint_emptyHint_throwsException() {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                    () -> gameService.validateHint("", 3, 1L));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertEquals("Hint cannot be empty and only one word is allowed", exception.getReason());
        }
        
        @Test
        public void validateHint_multiwordHint_throwsException() {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                    () -> gameService.validateHint("multiple words", 3, 1L));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertEquals("Hint cannot be empty and only one word is allowed", exception.getReason());
        }
        
        @Test
        public void validateHint_zeroWordCount_throwsException() {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                    () -> gameService.validateHint("forest", 0, 1L));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertEquals("Word count must be at least 1", exception.getReason());
        }
        
        @Test
        public void validateHint_gameNotFound_throwsException() {
            when(gameRepository.findById(anyLong())).thenReturn(Optional.empty());
            
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                    () -> gameService.validateHint("forest", 3, 1L));
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertEquals("Game not found", exception.getReason());
        }
    }
    
    @Nested
    class GuessTests {
        @Test
        public void makeGuess_correctTeamCard_success() {
            Game game = new Game();
            game.setId(1L);
            game.setTeamTurn(TeamColor.RED);
            game.setStatus("playing");
            game.setCurrentHint("hint", 2);

            // Setup lobby with teams
            Lobby dummyLobby = new Lobby();
            dummyLobby.setId(1L);
            Team redTeam = new Team();
            redTeam.setId(10L);
            Team blueTeam = new Team();
            blueTeam.setId(20L);
            dummyLobby.setRedTeam(redTeam);
            dummyLobby.setBlueTeam(blueTeam);

            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(dummyLobby));
            when(playerRepository.findByTeamId(10L)).thenReturn(List.of());
            when(playerRepository.findByTeamId(20L)).thenReturn(List.of());

            // Create board
            List<Card> board = new ArrayList<>();
            Card redCard = new Card("APPLE", CardColor.RED);
            board.add(redCard);
            game.setBoard(board);

            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

            Map.Entry<Boolean, TeamColor> result = gameService.makeGuess(1L, TeamColor.RED, "APPLE", new User());

            assertTrue(result.getKey());
            assertEquals(TeamColor.RED, result.getValue());
            assertTrue(redCard.isGuessed());
            assertEquals(1, game.getGuessedInHint());
        }
        
        @Test
        public void makeGuess_enemyCard_switchesTurn() {
            // Setup game
            Game game = new Game();
            game.setId(1L);
            game.setTeamTurn(TeamColor.RED);
            game.setStatus("playing");
            game.setCurrentHint("hint", 2);
            
            // Create a board with a blue card
            List<Card> board = new ArrayList<>();
            Card blueCard = new Card("APPLE", CardColor.BLUE);
            Card blueCard2 = new Card("BANANA", CardColor.BLUE);
            board.add(blueCard);
            board.add(blueCard2);
            game.setBoard(board);
            
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            
            // Make the guess
            Map.Entry<Boolean, TeamColor> result = gameService.makeGuess(1L, TeamColor.RED, "APPLE", new User());
            
            // Verify results
            assertFalse(result.getKey()); // Game not over
            assertEquals(TeamColor.BLUE, result.getValue()); // Turn switched to BLUE team
            assertTrue(blueCard.isGuessed()); // Card marked as guessed
            assertEquals(TeamColor.BLUE, game.getTeamTurn()); // Team turn updated
        }

        @Test
        public void makeGuess_blackCard_gameOver() {
            Game game = new Game();
            game.setId(1L);
            game.setTeamTurn(TeamColor.RED);
            game.setStatus("playing");
            game.setCurrentHint("hint", 2);

            Lobby dummyLobby = new Lobby();
            dummyLobby.setId(1L);
            Team redTeam = new Team();
            redTeam.setId(10L);
            Team blueTeam = new Team();
            blueTeam.setId(20L);
            dummyLobby.setRedTeam(redTeam);
            dummyLobby.setBlueTeam(blueTeam);

            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(dummyLobby));
            when(playerRepository.findByTeamId(10L)).thenReturn(List.of());
            when(playerRepository.findByTeamId(20L)).thenReturn(List.of());

            List<Card> board = new ArrayList<>();
            Card blackCard = new Card("APPLE", CardColor.BLACK);
            board.add(blackCard);
            game.setBoard(board);

            User user = new User();

            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            when(userRepository.save(any(User.class))).thenReturn(user);

            Map.Entry<Boolean, TeamColor> result = gameService.makeGuess(1L, TeamColor.RED, "APPLE", user);

            assertTrue(result.getKey());
            assertEquals(TeamColor.BLUE, result.getValue());
            assertTrue(blackCard.isGuessed());
            assertEquals("finished", game.getStatus());
            assertEquals(TeamColor.BLUE, game.getWinningTeam());
            verify(userRepository).save(user);
        }
        
        @Test
        public void makeGuess_neutralCard_switchesTurn() {
            // Setup game
            Game game = new Game();
            game.setId(1L);
            game.setTeamTurn(TeamColor.RED);
            game.setStatus("playing");
            game.setCurrentHint("hint", 2);
            
            // Create a board with a neutral card
            List<Card> board = new ArrayList<>();
            Card neutralCard = new Card("APPLE", CardColor.NEUTRAL);
            board.add(neutralCard);
            game.setBoard(board);
            
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            
            // Make the guess
            Map.Entry<Boolean, TeamColor> result = gameService.makeGuess(1L, TeamColor.RED, "APPLE", new User());
            
            // Verify results
            assertFalse(result.getKey()); // Game not over
            assertEquals(TeamColor.BLUE, result.getValue()); // Turn switched to BLUE team
            assertTrue(neutralCard.isGuessed()); // Card marked as guessed
        }

        @Test
        public void makeGuess_lastCardOfTeam_gameOver() {
            Game game = new Game();
            game.setId(1L);
            game.setTeamTurn(TeamColor.RED);
            game.setStatus("playing");
            game.setCurrentHint("hint", 2);

            Lobby dummyLobby = new Lobby();
            dummyLobby.setId(1L);
            Team redTeam = new Team();
            redTeam.setId(10L);
            Team blueTeam = new Team();
            blueTeam.setId(20L);
            dummyLobby.setRedTeam(redTeam);
            dummyLobby.setBlueTeam(blueTeam);

            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(dummyLobby));
            when(playerRepository.findByTeamId(10L)).thenReturn(List.of());
            when(playerRepository.findByTeamId(20L)).thenReturn(List.of());

            List<Card> board = new ArrayList<>();
            Card redCard = new Card("APPLE", CardColor.RED);
            board.add(redCard);
            game.setBoard(board);

            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

            Map.Entry<Boolean, TeamColor> result = gameService.makeGuess(1L, TeamColor.RED, "APPLE", new User());

            assertTrue(result.getKey());
            assertEquals(TeamColor.RED, result.getValue());
            assertTrue(redCard.isGuessed());
            assertEquals("finished", game.getStatus());
            assertEquals(TeamColor.RED, game.getWinningTeam());
        }
        
        @Test
        public void makeGuess_notYourTurn_throwsException() {
            // Setup game
            Game game = new Game();
            game.setId(1L);
            game.setTeamTurn(TeamColor.RED);
            
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            
            // Try to make a guess as BLUE team
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> gameService.makeGuess(1L, TeamColor.BLUE, "APPLE", new User()));
            
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
            assertEquals("It's not your turn", exception.getReason());
        }
        
        @Test
        public void makeGuess_gameFinished_throwsException() {
            // Setup game
            Game game = new Game();
            game.setId(1L);
            game.setTeamTurn(TeamColor.RED);
            game.setStatus("finished");
            game.setCurrentHint("hint", 2);
            
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            
            // Try to make a guess in a finished game
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> gameService.makeGuess(1L, TeamColor.RED, "APPLE", new User()));
            
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
            assertEquals("Game is already finished", exception.getReason());
        }
        
        @Test
        public void makeGuess_exceedMaxGuesses_throwsException() {
            // Setup game
            Game game = new Game();
            game.setId(1L);
            game.setTeamTurn(TeamColor.RED);
            game.setStatus("playing");
            game.setCurrentHint("hint", 2);
            game.setGuessedInHint(2); // Already guessed 2 times
            
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            
            // Try to make an additional guess
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> gameService.makeGuess(1L, TeamColor.RED, "APPLE", new User()));
            
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
            assertEquals("You have already guessed the maximum number of words for this hint", exception.getReason());
        }
        
        @Test
        public void makeGuess_wordNotFound_throwsException() {
            // Setup game
            Game game = new Game();
            game.setId(1L);
            game.setTeamTurn(TeamColor.RED);
            game.setStatus("playing");
            game.setCurrentHint("hint", 2);
            game.setBoard(new ArrayList<>()); // Empty board
            
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            
            // Try to guess a word that's not on the board
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> gameService.makeGuess(1L, TeamColor.RED, "NONEXISTENT", new User()));
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertEquals("Word not found in the game board", exception.getReason());
        }
    }
    
    @Nested
    class PlayerStatsTests {
        @Test
        public void updatePlayerStats_updatesWinLossRecords() {
            // Setup lobby with players
            Lobby lobby = new Lobby();
            lobby.setId(1L);
            
            // Create teams
            Team redTeam = new Team();
            redTeam.setColor(TeamColor.RED);
            Team blueTeam = new Team();
            blueTeam.setColor(TeamColor.BLUE);
            
            // Create players
            Player redPlayer1 = new Player();
            redPlayer1.setId(1L);
            redPlayer1.setTeam(redTeam);
            
            Player redPlayer2 = new Player();
            redPlayer2.setId(2L);
            redPlayer2.setTeam(redTeam);
            
            Player bluePlayer1 = new Player();
            bluePlayer1.setId(3L);
            bluePlayer1.setTeam(blueTeam);
            
            Player bluePlayer2 = new Player();
            bluePlayer2.setId(4L);
            bluePlayer2.setTeam(blueTeam);
            
            lobby.addPlayer(redPlayer1);
            lobby.addPlayer(redPlayer2);
            lobby.addPlayer(bluePlayer1);
            lobby.addPlayer(bluePlayer2);
            
            // Create users
            User user1 = new User();
            user1.setId(1L);
            user1.setLosses(0);
            user1.setWins(0);
            User user2 = new User();
            user2.setId(2L);
            user2.setLosses(0);
            user2.setWins(0);
            User user3 = new User();
            user3.setId(3L);
            user3.setLosses(0);
            user3.setWins(0);
            User user4 = new User();
            user4.setId(4L);
            user4.setLosses(0);
            user4.setWins(0);
            
            // Mock repository methods
            when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
            when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
            when(userRepository.findById(4L)).thenReturn(Optional.of(user4));
            
            // Execute the method - RED team wins
            gameService.updatePlayerStats(1L, TeamColor.RED);
            
            // Verify RED team players got a win
            verify(userRepository).save(eq(user1));
            verify(userRepository).save(eq(user2));
            
            // Verify BLUE team players got a loss
            verify(userRepository).save(eq(user3));
            verify(userRepository).save(eq(user4));
            
            // Verify win/loss counts
            assertEquals(1, user1.getWins());
            assertEquals(0, user1.getLosses());
            assertEquals(1, user2.getWins());
            assertEquals(0, user2.getLosses());
            assertEquals(0, user3.getWins());
            assertEquals(1, user3.getLosses());
            assertEquals(0, user4.getWins());
            assertEquals(1, user4.getLosses());
        }
        
        @Test
        public void updatePlayerStats_lobbyNotFound_throwsException() {
            when(lobbyRepository.findById(anyLong())).thenReturn(Optional.empty());
            
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                    () -> gameService.updatePlayerStats(1L, TeamColor.RED));
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertEquals("Lobby not found", exception.getReason());
        }
    }

    @Test
    public void endTurn_validCall_switchesTurnAndResetsGuesses() {
        Game game = new Game();
        game.setId(1L);
        game.setTeamTurn(TeamColor.RED);
        game.setGuessedInHint(2);
        game.setCurrentHint("hint", 3);

        List<Card> board = new ArrayList<>();
        board.add(new Card("APPLE", CardColor.RED)); 
        game.setBoard(board);

        User user = new User();
        user.setId(1L);

        Player player = new Player();
        player.setId(1L); 

        Lobby lobby = new Lobby();
        lobby.setId(1L);
        Team redTeam = new Team();
        redTeam.setId(10L);
        Team blueTeam = new Team();
        blueTeam.setId(20L);
        lobby.setRedTeam(redTeam);
        lobby.setBlueTeam(blueTeam);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(playerRepository.findByTeamId(anyLong())).thenReturn(List.of());
        when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

        gameService.endTurn(1L, user);

        assertEquals(TeamColor.BLUE, game.getTeamTurn());
        assertEquals(0, game.getGuessedInHint());
        verify(gameRepository).save(game);
    }

    @Test
    public void getRemainingGuesses_withHint_returnsCorrectValue() {
        Game game = new Game();
        game.setId(1L);
        game.setCurrentHint("hint", 3);
        game.setGuessedInHint(1);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        int remaining = gameService.getRemainingGuesses(1L);

        assertEquals(2, remaining);
    }

    @Test
    public void getRemainingGuesses_withoutHint_throwsException() {
        Game game = new Game();
        game.setId(1L);
        // Do NOT set a current hint at all — leave it null

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            gameService.getRemainingGuesses(1L);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("No hint has been given yet", ex.getReason());
    }

}

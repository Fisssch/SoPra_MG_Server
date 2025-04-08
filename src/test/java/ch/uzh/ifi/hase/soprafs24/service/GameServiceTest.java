package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private GameService gameService;

    private Game game;
    private Lobby lobby;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

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

        Game result = gameService.startOrGetGame(1L, TeamColor.RED, GameMode.CLASSIC, "default");

        assertEquals(game.getId(), result.getId());
        verify(gameRepository, never()).save(game); // Should not save because already exists
    }

    @Test
    public void startOrGetGame_newGame_createdSuccessfully() {
        Game createdGame = new Game();
        createdGame.setId(1L);
    
        when(gameRepository.findById(1L))
            .thenReturn(Optional.empty())  // first call -> no game found
            .thenReturn(Optional.of(createdGame)); // second call -> return newly created game
    
        when(gameRepository.save(any(Game.class))).thenAnswer(i -> {
            Game g = (Game) i.getArguments()[0];
            g.setId(1L); 
            return g;
        });
    
        when(wordGenerationService.getWordsFromApi()).thenReturn(Arrays.asList(
            "apple", "banana", "cherry", "dog", "cat", "tree", "house", "river",
            "car", "mountain", "bird", "school", "computer", "book", "phone",
            "chair", "sun", "moon", "star", "water", "pen", "desk", "cloud",
            "road", "train"
        )); 
    
        Lobby dummyLobby = new Lobby();
        dummyLobby.setCustomWords(new ArrayList<>());
        dummyLobby.setGameMode(GameMode.CLASSIC);
        when(lobbyRepository.findById(1L)).thenReturn(Optional.of(dummyLobby));
    
        Game result = gameService.startOrGetGame(1L, TeamColor.RED, GameMode.CLASSIC, "default");
    
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

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

        Lobby lobby = new Lobby();
        lobby.setCustomWords(Arrays.asList("custom1", "custom2"));
        lobby.setGameMode(GameMode.OWN_WORDS);
        when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

        when(wordGenerationService.getWordsFromApi()).thenReturn(Arrays.asList(
            "word1", "word2", "word3", "word4", "word5", "word6", "word7", "word8",
            "word9", "word10", "word11", "word12", "word13", "word14", "word15", "word16",
            "word17", "word18", "word19", "word20", "word21", "word22", "word23", "word24", "word25"
        ));

        List<String> result = gameService.generateWords(1L, "default");

        assertNotNull(result);
        assertEquals(25, result.size());
        assertTrue(result.contains("CUSTOM1"));
        assertTrue(result.contains("CUSTOM2"));
    }


    @Test
    public void generateWords_gameNotFound_throwsException() {
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> gameService.generateWords(99L, "default"));
    }

    @Test
    public void generateWords_lobbyNotFound_throwsException() {
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(lobbyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> gameService.generateWords(1L, "default"));
    }
    
}

package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.CardColor;
import ch.uzh.ifi.hase.soprafs24.constant.GameLanguage;
import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.SelectWordDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.makeGuessDTO;

@Service
@Transactional
public class GameService {
  
    private final Logger log = LoggerFactory.getLogger(GameService.class);
    private final WordGenerationService wordGenerationService;
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final LobbyRepository lobbyRepository;
    private final LobbyService lobbyService;
    private final WebsocketService websocketService;
    private final Map<Long, Object> locks = new ConcurrentHashMap<>();
    private final Map<Long, Timer> turnTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public GameService(
            WordGenerationService wordGenerationService,
            GameRepository gameRepository,
            PlayerRepository playerRepository,
            UserRepository userRepository,
            LobbyRepository lobbyRepository,
            LobbyService lobbyService,
            WebsocketService websocketService
    ) {
        this.wordGenerationService = wordGenerationService;
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.userRepository = userRepository;
        this.lobbyRepository = lobbyRepository;
        this.lobbyService = lobbyService;
        this.websocketService = websocketService;
    }

    public void checkIfUserSpymaster(User user) {
        Player player = playerRepository.findById(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
        if (player.getRole() != PlayerRole.SPYMASTER) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only spymasters can give hints");
        }
    }
    
    public void validateHint(String hint, Integer wordCount, Long gameId) {
        if (hint == null || hint.isEmpty() || hint.contains(" ")) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hint cannot be empty and only one word is allowed");
        }
        if (wordCount == null || wordCount < 1) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Word count must be at least 1");
        }
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        
        // Check if the hint matches any word on the board
        List<Card> board = game.getBoard();
        for (Card card : board) {
            if (card.getWord().equalsIgnoreCase(hint)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hint cannot be the same as a word on the board");
            }
        }
        
        
        
        game.setCurrentHint(hint, wordCount);
        game.setGuessedInHint(0); //reset guessed words for the new hint
        gameRepository.save(game);        
    }

    public Game startOrGetGame(Long id, TeamColor startingTeam, GameMode gameMode) {
        //first check if game already present 
        Optional<Game> optionalGame = gameRepository.findById(id);
        if (optionalGame.isPresent()){
            return optionalGame.get();
        } 

        //per-id lock to avoid race conditions 
        Object lock = locks.computeIfAbsent(id, k -> new Object()); 
        synchronized (lock) {
            try {
                //again check if game was created while waiting 
                optionalGame = gameRepository.findById(id);
                if (optionalGame.isPresent()) {
                    return optionalGame.get();
                }
                Lobby lobby = lobbyRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
                String theme = lobby.getTheme(); 
                GameLanguage language = lobby.getLanguage();
                Integer turnDuration = lobby.getTurnDuration();

                GameMode actualMode = lobby.getGameMode();

                if (theme != null && !theme.equalsIgnoreCase("default") && actualMode != GameMode.THEME) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Theme provided but game mode is not THEME");
                }

                Game game = new Game();
                game.setId(id);
                game.setStartingTeam(startingTeam);
                game.setTeamTurn(startingTeam); 
                game.setStatus("playing");
                game.setWinningTeam(null);
                game.setGameMode(gameMode);
                game.setTurnDuration(turnDuration);

                try {
                    List <String> words = generateWords(game, theme, language); 
                    game.setWords(words);
        
                    List <Card> board = assignColorsToWords(words, startingTeam);
                    game.setBoard(board);
        
                    gameRepository.save(game);
                    setTurnTimerIfNeeded(game); 
                    return game; 

                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to create a new game: " + e.getMessage(), e);
                }
            } finally {
                locks.remove(id); 
            }
        }
    }

    public List<Card> getBoard(Long id) {
        Game game = gameRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        return game.getBoard();
    }

    /*
     * Returns a tuple with a bool which indicates if the game is over and a TeamColor which team has either won or whose turn it is next.
    */
    public Map.Entry<Boolean, TeamColor> makeGuess(Long id, TeamColor teamColor, String wordStr, User user) {
        Game game = gameRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        if (game.getTeamTurn() != teamColor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It's not your turn");
        }
        if (game.getCurrentHint() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have to give a hint before guessing");
        }
        if (game.getGuessedInHint() >= game.getCurrentHint().getValue()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have already guessed the maximum number of words for this hint");
        }
        if (game.getStatus().equalsIgnoreCase("finished")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Game is already finished");
        }
        Map.Entry<Boolean, TeamColor> result;
        Card word = findWord(game.getBoard(), wordStr);
        var opponentTeam = teamColor == TeamColor.RED ? TeamColor.BLUE : TeamColor.RED;
        boolean scheduleTimer = false;

        // Black card guess
        if (word.getColor() == CardColor.BLACK) {
            word.setGuessed(true);
            game.setWinningTeam(opponentTeam);
            game.setStatus("finished");
            resetLobbyGameStarted(game.getId()); //reset gameStarted state in loby 
            user.addBlackCardGuess();
            userRepository.save(user);
            result = Map.entry(true, opponentTeam);
            scheduleGameDeletion(game.getId()); //delete game 
        } 
        // Neutral card guess
        else if (word.getColor() == CardColor.NEUTRAL) {
            word.setGuessed(true);

            // Clear temp word selection 
            for (Card card : game.getBoard()) {
                card.setSelected(false);
            }

            game.setTeamTurn(opponentTeam);
            scheduleTimer = true;
            result = Map.entry(false, opponentTeam);
        } 
        // Enemy card guess
        else if (word.getColor().name() != teamColor.name()) {
            word.setGuessed(true);

            // Clear temp word selection 
            for (Card card : game.getBoard()) {
                card.setSelected(false);
            }

            game.setTeamTurn(opponentTeam);
            Long leftToGuess = game.getBoard().stream()
                .filter(card -> card.getColor() == word.getColor() && !card.isGuessed())
                .count();
            if (leftToGuess == 0) {
                game.setWinningTeam(opponentTeam);
                game.setStatus("finished");
                resetLobbyGameStarted(game.getId()); //reset gameStarted state in loby 
                result = Map.entry(true, opponentTeam);
                scheduleGameDeletion(game.getId()); //delete game 
            } else {
                scheduleTimer = true;
                result = Map.entry(false, opponentTeam);
            }
        }
        // Correct card guess
        else {
          word.setGuessed(true);
          game.addGuessedInHint();
          Long leftToGuess = game.getBoard().stream()
              .filter(card -> card.getColor() == word.getColor() && !card.isGuessed())
              .count();
          if (leftToGuess == 0) {
              game.setWinningTeam(teamColor);
              game.setStatus("finished");
              resetLobbyGameStarted(game.getId()); //reset gameStarted state in loby 
              result = Map.entry(true, teamColor);
              scheduleGameDeletion(game.getId()); //delete game 
          } else {
              if (game.getGuessedInHint() >= game.getCurrentHint().getValue()) {

                // Clear temp word selection 
                for (Card card : game.getBoard()) {
                    card.setSelected(false);
                }

                  game.setTeamTurn(opponentTeam);
                  scheduleTimer = true;
                  result = Map.entry(false, opponentTeam);
              }
              else
                result = Map.entry(false,  teamColor);
          }
        }
        gameRepository.save(game);
        if (scheduleTimer)
            setTurnTimerIfNeeded(game);
        return result;
    }

    public void updatePlayerStats(Long id, TeamColor teamColor) {
        Lobby lobby = lobbyRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
        List<Player> players = lobby.getPlayers();
        for (Player player : players) {
            User user = userRepository.findById(player.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            if (player.getTeam().getColor() == teamColor) {
                user.addWin();
            } else {
                user.addLoss();
            }
            userRepository.save(user);
        }
    }

    public void scheduleGameDeletion(Long gameId) {
        scheduler.schedule(() -> {
            try {
                stopTurnTimer(gameId);
                gameRepository.deleteById(gameId);
                System.out.println("Game with ID " + gameId + " has been deleted after game end.");
            } catch (Exception e) {
                System.err.println("Failed to delete game with ID " + gameId + ": " + e.getMessage());
            }
        }, 3, TimeUnit.SECONDS); // Delay of 3 seconds
    }

    /////////////////////// helper methods: ///////////////////////
    private List<Card> assignColorsToWords(List<String> words, TeamColor startingTeam) {

        Collections.shuffle(words);

        List<Card> board = new ArrayList<>();

        CardColor startingColor = (startingTeam == TeamColor.RED) ? CardColor.RED : CardColor.BLUE; //if true assign red to starting color and other color = blue
        CardColor otherColor = (startingColor == CardColor.RED) ? CardColor.BLUE : CardColor.RED; //if starting color = red then other = blue, if starting color = blue then other = red 

        int index = 0;

        // 9 cards to starting team
        for (int i = 0; i < 9; i++) {
            board.add(new Card(words.get(index++), startingColor));
        }
        // 8 cards to the other team
        for (int i = 0; i < 8; i++) {
            board.add(new Card(words.get(index++), otherColor));
        }
        // 7 neutral cards
        for (int i = 0; i < 7; i++) {
            board.add(new Card(words.get(index++), CardColor.NEUTRAL));
        }
        // 1 black card
        board.add(new Card(words.get(index), CardColor.BLACK));

        Collections.shuffle(board);

        return board;
        }

    public List<String> generateWords(Game game, String theme, GameLanguage language){
        if (game.getWords() != null && !game.getWords().isEmpty()){
            return game.getWords(); 
        }

        Lobby lobby = lobbyRepository.findById(game.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
        List<String> finalWords = new ArrayList<>();
        
        if (lobby.getCustomWords() != null && lobby.getGameMode() == GameMode.OWN_WORDS) { 
          for (String word : lobby.getCustomWords()){
            String upper = word.toUpperCase();
            if (!finalWords.contains(upper)){
                finalWords.add(upper); 
            }
          }
        }
        int needed = 25 - finalWords.size(); 
        if(needed > 0) {
            List<String> additional = theme == null || theme.equalsIgnoreCase("default") ?
            wordGenerationService.getWordsFromApi(language) : wordGenerationService.getWordsFromApi(theme, language); //if theme is missing or default call getWordsFromApi() else getWordsFromApi(theme)

            for (String w : additional){
              String upper = w.toUpperCase();
              if (!finalWords.contains(upper)){
                finalWords.add(upper);
                if (finalWords.size() == 25) break;
              }
              
            }
        }
        game.setWords(finalWords);
        return finalWords;
    }

    private Card findWord(List<Card> words, String word) {
        for (Card card : words) {
          if (card.getWord().equalsIgnoreCase(word)) {
              return card;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found in the game board");
    }

    private void resetLobbyGameStarted(Long gameId) {
        Lobby lobby = lobbyRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
        lobby.setGameStarted(false);
        lobby.setCreatedAt(java.time.Instant.now()); // optional: Reset countdown
        lobbyRepository.save(lobby);

        Long redTeamId = lobby.getRedTeam().getId();
        Long blueTeamId = lobby.getBlueTeam().getId();

        List<Player> redPlayers = playerRepository.findByTeamId(redTeamId);
        List<Player> bluePlayers = playerRepository.findByTeamId(blueTeamId);

        for (Player player : redPlayers) {
            player.setReady(false);
            playerRepository.save(player);
        }

        for (Player player : bluePlayers) {
            player.setReady(false);
            playerRepository.save(player);
        }
    }
    public int getRemainingGuesses(Long gameId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        if (game.getCurrentHint() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hint has been given yet");
        }

        return game.getCurrentHint().getValue() - game.getGuessedInHint();
    }
    public void endTurn(Long gameId, User user) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
    
        // Ensure the user is part of the game
        Player player = playerRepository.findById(user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
        
    
        // Switch the turn to the opposing team
        TeamColor currentTeam = game.getTeamTurn();
        TeamColor nextTeam = (currentTeam == TeamColor.RED) ? TeamColor.BLUE : TeamColor.RED;
        game.setTeamTurn(nextTeam);
    
        // Reset guesses left to the number of words in the current hint
        if (game.getCurrentHint() != null) {
            game.setGuessedInHint(0); // Reset guessed words
        }

        // Clear temp selection 
        for (Card card : game.getBoard()) {
            card.setSelected(false);
        }
    
        gameRepository.save(game);
    }

    public Game getGameById(Long gameId) {
        return gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
    }

    public void selectWord(Long gameId, SelectWordDTO selectWordDTO) {
        // get game
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
    
        // get card 
        Card selectedCard = findWord(game.getBoard(), selectWordDTO.getWordStr());
        
        // update selection state of card 
        selectedCard.setSelected(selectWordDTO.isSelected()); 
    
        // save updated game 
        gameRepository.save(game);
    }

    //private Card findCardByWord(List<Card> board, String word) {
    //    return board.stream()
    //        .filter(card -> card.getWord().equalsIgnoreCase(word))
    //        .findFirst()
    //        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found on the board"));
    //}

    public void checkIfUserIsFieldOperative(Long userId, TeamColor teamColor) {
        Player player = playerRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
    
        if (player.getRole() != PlayerRole.FIELD_OPERATIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only field operatives can select words temporarily");
        }
    
        if (player.getTeam().getColor() != teamColor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only select words for your own team");
        }
    }

    private void setTurnTimerIfNeeded(Game game) {
        if (game.getGameMode() != GameMode.TIMED)
           return;
        
        // timer already set
        if (turnTimers.containsKey(game.getId())) 
            return;
        
        Timer timer = new Timer();
        turnTimers.put(game.getId(), timer);
        
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    game.setTeamTurn(game.getTeamTurn() == TeamColor.RED ? TeamColor.BLUE : TeamColor.RED);
                    gameRepository.save(game);
                    websocketService.sendMessage("/topic/game/" + game.getId() + "/guess", new makeGuessDTO(game.getTeamTurn().name(), ""));
                    stopTurnTimer(game.getId());
                    setTurnTimerIfNeeded(game);
                } catch (Exception e) {
                    log.warn("Error in scheduled turn change for game {}: {}", game.getId(), e.getMessage());
                } finally {
                    try {
                        stopTurnTimer(game.getId());
                    } catch (Exception e) {
                        log.warn("Failed to remove timer for game {}: {}", game.getId(), e.getMessage());
                    }
                }
            }
        }, game.getTurnDuration() * 1000);
    }

    private void stopTurnTimer(Long gameId) {
        Timer timer = turnTimers.remove(gameId);
        if (timer != null) {
            timer.cancel();
        }
    }
}
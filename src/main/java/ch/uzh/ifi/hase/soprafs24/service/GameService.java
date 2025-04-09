package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.*;
import ch.uzh.ifi.hase.soprafs24.entity.*;

import ch.uzh.ifi.hase.soprafs24.repository.*;

@Service
@Transactional
public class GameService {
  
  private final Logger log = LoggerFactory.getLogger(GameService.class);
  private final WordGenerationService wordGenerationService;
  private final GameRepository gameRepository;
  private final PlayerRepository playerRepository;
  private final UserRepository userRepository;
  private final LobbyRepository lobbyRepository;

  public GameService(WordGenerationService wordGenerationService, GameRepository gameRepository, PlayerRepository playerRepository, UserRepository userRepository, LobbyRepository lobbyRepository) {
    this.wordGenerationService = wordGenerationService;
    this.gameRepository = gameRepository;
    this.playerRepository = playerRepository;
    this.userRepository = userRepository;
    this.lobbyRepository = lobbyRepository;
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
    game.setCurrentHint(hint, wordCount);
    gameRepository.save(game);
  }

  public Game startOrGetGame(Long id, TeamColor startingTeam, GameMode gameMode, String theme) {
    Optional<Game> optionalGame = gameRepository.findById(id);
    Game game;
        
    if (optionalGame.isPresent()){
        game = optionalGame.get();
    } else {
        game = new Game();
        game.setId(id);
        game.setStartingTeam(startingTeam);
        game.setTeamTurn(startingTeam); 
        game.setStatus("playing");
        game.setWinningTeam(null);
        game.setGameMode(gameMode);
        gameRepository.save(game); 

        List <String> words = generateWords(id, theme); 
        game.setWords(words);
        List <Card> board = assignColorsToWords(words, startingTeam);
        game.setBoard(board);

        gameRepository.save(game);
    }
    return game; 
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

        // Black card guess
        if (word.getColor() == CardColor.BLACK) {
            word.setGuessed(true);
            game.setWinningTeam(opponentTeam);
            game.setStatus("finished");
            user.addBlackCardGuess();
            userRepository.save(user);
            result = Map.entry(true, opponentTeam);
        } 
        // Neutral card guess
        else if (word.getColor() == CardColor.NEUTRAL) {
            word.setGuessed(true);
            game.setTeamTurn(opponentTeam);
            result = Map.entry(false, opponentTeam);
        } 
        // Enemy card guess
        else if (word.getColor().name() != teamColor.name()) {
            word.setGuessed(true);
            game.setTeamTurn(opponentTeam);
            Long leftToGuess = game.getBoard().stream()
                .filter(card -> card.getColor() == word.getColor() && !card.isGuessed())
                .count();
            if (leftToGuess == 0) {
                game.setWinningTeam(opponentTeam);
                game.setStatus("finished");
                result = Map.entry(true, opponentTeam);
            } else
                result = Map.entry(false, opponentTeam);
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
              result = Map.entry(true, teamColor);
          } else {
              if (game.getGuessedInHint() >= game.getCurrentHint().getValue()) {
                  game.setTeamTurn(opponentTeam);
                  result = Map.entry(false,  opponentTeam);
              }
              else
                result = Map.entry(false,  teamColor);
          }
        }
        gameRepository.save(game);
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

    public List<String> generateWords(Long id, String theme){
        Game game = gameRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game with id " + id + " not found."));
        
        if (game.getWords() != null && !game.getWords().isEmpty()){
            return game.getWords(); 
        }

        Lobby lobby = lobbyRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
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
            wordGenerationService.getWordsFromApi() : wordGenerationService.getWordsFromApi(theme); //if theme is missing or default call getWordsFromApi() else getWordsFromApi(theme)

            for (String w : additional){
              String upper = w.toUpperCase();
              if (!finalWords.contains(upper)){
                finalWords.add(upper);
                if (finalWords.size() == 25) break;
              }
              
            }
        }
        game.setWords(finalWords);
        gameRepository.save(game);
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
}

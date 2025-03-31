package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.CardColor;
import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.service.WordGenerationService;
import ch.uzh.ifi.hase.soprafs24.constant.TeamColor;




@Service
@Transactional
public class GameService {

  private final Logger log = LoggerFactory.getLogger(GameService.class);
  private final WordGenerationService wordGenerationService;
  private final GameRepository gameRepository;

  @Autowired
  public GameService(WordGenerationService wordGenerationService, GameRepository gameRepository) {
    this.wordGenerationService = wordGenerationService;
    this.gameRepository = gameRepository;
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
        game = gameRepository.save(game); 

        List <String> words = generateWords(id, theme); 
        game.setWords(words);
        List <Card> board = assignColorsToWords(words, startingTeam);
        game.setBoard(board);

        game = gameRepository.save(game);
    }
    return game; 
    }

    public List<Card> getBoard(Long id) {
        Game game = gameRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        return game.getBoard();
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
        Game game = gameRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game with id " + id + " not found."));
        if (game.getWords() != null && !game.getWords().isEmpty()){
            return game.getWords(); 
        }
        List<String> words;
        if (theme == null || theme.equalsIgnoreCase("default")){
            words = wordGenerationService.getWordsFromApi();
        } else {
            words = wordGenerationService.getWordsFromApi(theme);
        }
        return words; 
    }
}

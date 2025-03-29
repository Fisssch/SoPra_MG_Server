package ch.uzh.ifi.hase.soprafs24.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.service.WordGenerationService;



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

  public List<String> generateWords(Long id){
    Game game = gameRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game with id " + id + " not found."));
    if (game.getWords() != null && !game.getWords().isEmpty()){
        return game.getWords(); 
    }
    List<String> words = wordGenerationService.getWordsFromApi();
    game.setWords(words);
    gameRepository.save(game); 
    return words; 
  }

  
}
package ch.uzh.ifi.hase.soprafs24.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.TeamRepository;

@Service
@Transactional
public class GameService {
  private final Logger log = LoggerFactory.getLogger(GameService.class);

  private final GameRepository gameRepository;
  private final TeamRepository teamRepository;

  @Autowired
  public GameService(GameRepository gameRepository, TeamRepository teamRepository) {
    this.gameRepository = gameRepository;
    this.teamRepository = teamRepository;
  }
      
  public void validateHint(String hint, Integer wordCount, Long teamId, Long gameId) {
    if (hint == null || hint.isEmpty() || hint.contains(" ")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hint cannot be empty");
    }
    if (wordCount == null || wordCount < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Word count must be at least 1");
    }
    var team = teamRepository.findById(teamId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
    var game = gameRepository.findById(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
    game.setCurrentHint(hint, wordCount);
    game.setGuessingTeam(team);
    gameRepository.save(game);
  }
}

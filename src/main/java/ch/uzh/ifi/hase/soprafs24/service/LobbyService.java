package ch.uzh.ifi.hase.soprafs24.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;

@Service
@Transactional
public class LobbyService {
  private final Logger log = LoggerFactory.getLogger(LobbyService.class);
    
  public Lobby getOrCreateLobby() {
    return new Lobby(); // Todo get or create lobby
  }

  public Lobby setGameMode(Integer id, GameMode gameMode) {
    Lobby lobby = new Lobby(); // Todo get lobby by id
    lobby.setGameMode(gameMode);
    // Todo update database entry
    return lobby;
  }
}

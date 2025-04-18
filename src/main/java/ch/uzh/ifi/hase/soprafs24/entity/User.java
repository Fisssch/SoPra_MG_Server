package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "USER")
public class User extends DatabaseEntity {

  private static final long serialVersionUID = 1L;

  @Indexed(unique = true)
  private String username;

  private String token;

  private UserStatus onlineStatus;

  private String password; 
  
  private Integer wins;

  private Integer losses;

  private Integer blackCardGuesses;

  private Boolean ready;

  private LocalDateTime creationDate;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public UserStatus getOnlineStatus() {
    return onlineStatus;
  }

  public void setOnlineStatus (UserStatus status) {
    this.onlineStatus = status;
  }

  public Integer getWins() {
      return wins;
  }

  public void setWins(Integer wins) {
      this.wins = wins;
  }

  public void addWin() {
      if (this.wins == null) {
          this.wins = 1;
      } else {
          this.wins++;
      }
  }

  public Integer getLosses() {
      return losses;
  }

  public void setLosses(Integer losses) {
      this.losses = losses;
  }

  public void addLoss() {
      if (this.losses == null) {
          this.losses = 1;
      } else {
          this.losses++;
      }
  }

  public Integer getBlackCardGuesses() {
      return blackCardGuesses;
  }

  public void setBlackCardGuesses(Integer blackCardGuesses) {
      this.blackCardGuesses = blackCardGuesses;
  }

  public void addBlackCardGuess() {
      if (this.blackCardGuesses == null) {
          this.blackCardGuesses = 1;
      } else {
          this.blackCardGuesses++;
      }
  }

  public Boolean getReady() {
      return ready;
  }

  public void setReady(Boolean ready) {
      this.ready = ready;
  }

  public String getPassword(){
    return password;
  }

  public void setPassword(String password){
    this.password = password;
  }

  public LocalDateTime getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(LocalDateTime creationDate) {
    this.creationDate = creationDate;
  }
}

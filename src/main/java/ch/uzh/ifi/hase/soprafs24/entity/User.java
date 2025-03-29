package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Internal User Representation
 * This class composes the internal representation of the user and defines how
 * the user is stored in the database.
 * Every variable will be mapped into a database field with the @Column
 * annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unqiue across the database -> composes
 * the primary key
 */
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

  public Integer getLosses() {
      return losses;
  }

  public void setLosses(Integer losses) {
      this.losses = losses;
  }

  public Integer getBlackCardGuesses() {
      return blackCardGuesses;
  }

  public void setBlackCardGuesses(Integer blackCardGuesses) {
      this.blackCardGuesses = blackCardGuesses;
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

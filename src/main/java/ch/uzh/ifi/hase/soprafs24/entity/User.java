package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

import javax.persistence.*;
import java.io.Serializable;

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
@Entity
@Table(name = "USER")
public class User implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(unique = true)
  private String token;

  @Column(nullable = false)
  private UserStatus onlineStatus;

  @Column(nullable = false)
  private String password; 
  
  @Column
  private Integer wins;

  @Column
  private Integer losses;

  @Column
  private Integer blackCardGuesses;

  @Column
  private Boolean ready;



  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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
}

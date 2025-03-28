package ch.uzh.ifi.hase.soprafs24.rest.dto;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

public class UserGetDTO {

    private Long id;
    private String username;
    private UserStatus onlineStatus;
    private Integer wins; 
    private Integer losses; 
    private Integer blackCardGuesses;

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

    public UserStatus getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(UserStatus onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public Integer getWins(){
        return wins; 
    }

    public void setWins(Integer wins){
        this.wins = wins; 
    }

    public Integer getLosses(){
        return losses; 
    }

    public void setLosses(Integer losses){
        this.losses = losses; 
    }

    public Integer getBlackCardGuesses(){
        return blackCardGuesses; 
    }

    public void setBlackCardGuesses(Integer blackCardGuesses){
        this.blackCardGuesses = blackCardGuesses; 
    }
}
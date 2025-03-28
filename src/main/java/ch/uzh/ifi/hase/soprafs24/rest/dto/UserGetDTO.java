package ch.uzh.ifi.hase.soprafs24.rest.dto;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

public class UserGetDTO {

    private Long id;
    private String username;
    private UserStatus onlineStatus;

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
}
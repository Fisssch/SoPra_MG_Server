package ch.uzh.ifi.hase.soprafs24.rest.dto;


public class PlayerTeamDTO {

    private String color;

    public PlayerTeamDTO() {
    }

    public PlayerTeamDTO(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
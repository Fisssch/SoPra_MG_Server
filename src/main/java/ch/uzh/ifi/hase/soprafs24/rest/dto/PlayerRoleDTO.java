package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class PlayerRoleDTO {
    private String role;

    public PlayerRoleDTO() {}

    public PlayerRoleDTO(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
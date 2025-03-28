// LobbyResponseDTO.java
package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LobbyResponseDTO {
    private Long id;

    public LobbyResponseDTO(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class ChatMessageDTO {
    public String sender;
    public String message;
    
    public ChatMessageDTO(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }
}

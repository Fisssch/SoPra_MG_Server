package ch.uzh.ifi.hase.soprafs24.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

public class WebSocketServiceTest {

    private SimpMessagingTemplate messagingTemplate;
    private WebsocketService websocketService;

    @BeforeEach
    public void setup() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        websocketService = new WebsocketService();
        websocketService.simpMessagingTemplate = messagingTemplate; // manually inject mock
    }

    @Test
    public void sendMessage_sendsToCorrectTopic() {
        // Arrange
        String topic = "/topic/test";
        String message = "test-message";

        // Act
        websocketService.sendMessage(topic, message);

        // Assert
        verify(messagingTemplate).convertAndSend(topic, message);
    }
}

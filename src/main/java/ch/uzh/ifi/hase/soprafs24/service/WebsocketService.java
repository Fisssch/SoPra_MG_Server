package ch.uzh.ifi.hase.soprafs24.service;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class WebsocketService {
    @Autowired
    protected SimpMessagingTemplate simpMessagingTemplate;

    public void sendMessage(String topic, Object toSend) {
        simpMessagingTemplate.convertAndSend(topic, toSend);
    }
}
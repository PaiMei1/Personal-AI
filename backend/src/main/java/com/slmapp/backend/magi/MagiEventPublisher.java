package com.slmapp.backend.magi;

import com.slmapp.backend.magi.events.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MagiEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public MagiEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    private String topicFor(UUID messageId) {
        return "/topic/magi/" + messageId;
    }

    public void unitStatus(MagiUnitStatusEvent event) {
        messagingTemplate.convertAndSend(topicFor(event.messageId()), event);
    }

    public void unitResult(MagiUnitResultEvent event) {
        messagingTemplate.convertAndSend(topicFor(event.messageId()), event);
    }

    public void decision(MagiDecisionEvent event) {
        messagingTemplate.convertAndSend(topicFor(event.messageId()), event);
    }

    public void synthesis(MagiSynthesisEvent event) {
        messagingTemplate.convertAndSend(topicFor(event.messageId()), event);
    }
}

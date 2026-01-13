package com.studysync.recommendation.kafka;

import com.studysync.recommendation.dto.UserInteractionEvent;
import com.studysync.recommendation.service.InteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserInteractionConsumer {

    private final InteractionService interactionService;

    @KafkaListener(topics = "${app.kafka.topics.user-interaction}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeUserInteraction(UserInteractionEvent event) {
        try {
            log.info("Received user interaction event: userId={}, postId={}, type={}",
                    event.getUserId(), event.getPostId(), event.getInteractionType());

            interactionService.processInteractionEvent(event);

            log.debug("Successfully processed interaction event for user {} on post {}",
                    event.getUserId(), event.getPostId());
        } catch (Exception e) {
            log.error("Error processing user interaction event: {}", event, e);
            // You could implement dead letter queue here
        }
    }
}

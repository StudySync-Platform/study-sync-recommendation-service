package com.studysync.recommendation.kafka;

import com.studysync.recommendation.dto.PostRecommendationEvent;
import com.studysync.recommendation.dto.UserInteractionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.user-interaction}")
    private String userInteractionTopic;

    @Value("${app.kafka.topics.post-recommendation}")
    private String postRecommendationTopic;

    public void sendInteractionEvent(UserInteractionEvent event) {
        try {
            String key = event.getUserId() + "-" + event.getPostId();
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(userInteractionTopic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Sent interaction event: {} with offset: {}",
                            key, result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send interaction event: {}", key, ex);
                }
            });
        } catch (Exception e) {
            log.error("Error sending interaction event to Kafka", e);
        }
    }

    public void sendRecommendationEvent(PostRecommendationEvent event) {
        try {
            String key = String.valueOf(event.getUserId());
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(postRecommendationTopic, key,
                    event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent {} recommendations for user {} with offset: {}",
                            event.getRecommendations().size(),
                            event.getUserId(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send recommendation event for user: {}", key, ex);
                }
            });
        } catch (Exception e) {
            log.error("Error sending recommendation event to Kafka", e);
        }
    }
}

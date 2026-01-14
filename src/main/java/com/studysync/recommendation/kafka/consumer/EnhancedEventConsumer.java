package com.studysync.recommendation.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studysync.recommendation.dto.PostLifecycleEvent;
import com.studysync.recommendation.dto.UserInteractionEvent;
import com.studysync.recommendation.service.InteractionService;
import com.studysync.recommendation.service.PostScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Enhanced Kafka Consumer for user interaction and post lifecycle events.
 * 
 * Features:
 * - Idempotency checking via event IDs
 * - Manual acknowledgment for reliable processing
 * - Error handling with DLQ support
 * - Retry logic for transient failures
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnhancedEventConsumer {

    private final InteractionService interactionService;
    private final PostScoreService postScoreService;
    private final ObjectMapper objectMapper;
    private final IdempotencyService idempotencyService;

    /**
     * Consume user interaction events (like, view, comment, share, bookmark)
     */
    @KafkaListener(topics = "${app.kafka.topics.user-interaction}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeInteractionEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = null;
        try {
            UserInteractionEvent event = objectMapper.readValue(record.value(), UserInteractionEvent.class);
            eventId = extractEventId(event);

            log.info("Received interaction event: userId={}, postId={}, type={}, eventId={}",
                    event.getUserId(), event.getPostId(), event.getInteractionType(), eventId);

            // Idempotency check - skip if already processed
            if (eventId != null && idempotencyService.isProcessed(eventId)) {
                log.debug("Skipping duplicate event: {}", eventId);
                ack.acknowledge();
                return;
            }

            // Process the interaction
            interactionService.processInteractionEvent(event);

            // Mark as processed for idempotency
            if (eventId != null) {
                idempotencyService.markProcessed(eventId);
            }

            ack.acknowledge();
            log.debug("Successfully processed interaction event: {}", eventId);

        } catch (Exception e) {
            log.error("Error processing interaction event: offset={}, key={}, eventId={}",
                    record.offset(), record.key(), eventId, e);
            // Don't acknowledge - will be retried or sent to DLQ based on config
            throw new RuntimeException("Failed to process interaction event", e);
        }
    }

    /**
     * Consume post lifecycle events (created, updated, deleted)
     */
    @KafkaListener(topics = "${app.kafka.topics.post-lifecycle:post-lifecycle-events}", groupId = "${spring.kafka.consumer.group-id}-lifecycle", containerFactory = "kafkaListenerContainerFactory")
    public void consumePostLifecycleEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = null;
        try {
            PostLifecycleEvent event = objectMapper.readValue(record.value(), PostLifecycleEvent.class);
            eventId = event.getEventId();

            log.info("Received post lifecycle event: type={}, postId={}, authorId={}, eventId={}",
                    event.getEventType(), event.getPostId(), event.getAuthorId(), eventId);

            // Idempotency check
            if (eventId != null && idempotencyService.isProcessed(eventId)) {
                log.debug("Skipping duplicate lifecycle event: {}", eventId);
                ack.acknowledge();
                return;
            }

            // Process based on event type
            processPostLifecycleEvent(event);

            // Mark as processed
            if (eventId != null) {
                idempotencyService.markProcessed(eventId);
            }

            ack.acknowledge();
            log.debug("Successfully processed lifecycle event: {}", eventId);

        } catch (Exception e) {
            log.error("Error processing lifecycle event: offset={}, key={}, eventId={}",
                    record.offset(), record.key(), eventId, e);
            throw new RuntimeException("Failed to process lifecycle event", e);
        }
    }

    /**
     * Dead Letter Queue consumer for failed messages
     */
    @KafkaListener(topics = "${app.kafka.topics.dlq:recommendation-dlq}", groupId = "${spring.kafka.consumer.group-id}-dlq")
    public void consumeDlqEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.warn("DLQ message received: topic={}, offset={}, key={}, value={}",
                record.topic(), record.offset(), record.key(), record.value());

        // Log for manual inspection - could also store in a database
        // In production, you might want to:
        // 1. Store in a dead_letter_messages table
        // 2. Send an alert to monitoring
        // 3. Provide a UI for manual reprocessing

        ack.acknowledge();
    }

    private void processPostLifecycleEvent(PostLifecycleEvent event) {
        switch (event.getEventTypeEnum()) {
            case POST_CREATED:
            case POST_PUBLISHED:
                postScoreService.initializePostScore(
                        event.getPostId(),
                        event.getAuthorId(),
                        event.getCategory());
                break;

            case POST_UPDATED:
                postScoreService.updatePostMetadata(
                        event.getPostId(),
                        event.getCategory());
                break;

            case POST_DELETED:
            case POST_UNPUBLISHED:
                postScoreService.removePostScore(event.getPostId());
                break;

            default:
                log.warn("Unknown post lifecycle event type: {}", event.getEventType());
        }
    }

    private String extractEventId(UserInteractionEvent event) {
        if (event.getMetadata() != null && event.getMetadata().containsKey("eventId")) {
            return event.getMetadata().get("eventId").toString();
        }
        // Fallback: generate deterministic ID from event properties
        return String.format("%s-%s-%s-%s",
                event.getUserId(),
                event.getPostId(),
                event.getInteractionType(),
                event.getTimestamp() != null ? event.getTimestamp().toString() : "null");
    }
}

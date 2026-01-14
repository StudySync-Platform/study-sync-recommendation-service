package com.studysync.recommendation.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service for tracking processed event IDs to ensure idempotency.
 * Uses Redis for distributed state across multiple consumer instances.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "processed_event:";
    private static final Duration TTL = Duration.ofHours(24); // Keep for 24 hours

    /**
     * Check if an event has already been processed
     */
    public boolean isProcessed(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            return false;
        }

        try {
            String key = KEY_PREFIX + eventId;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Failed to check idempotency for event {}: {}", eventId, e.getMessage());
            // Fail open - process the event if Redis is unavailable
            return false;
        }
    }

    /**
     * Mark an event as processed
     */
    public void markProcessed(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            return;
        }

        try {
            String key = KEY_PREFIX + eventId;
            redisTemplate.opsForValue().set(key, "1", TTL);
            log.debug("Marked event as processed: {}", eventId);
        } catch (Exception e) {
            log.warn("Failed to mark event as processed {}: {}", eventId, e.getMessage());
            // Continue - worst case is duplicate processing
        }
    }

    /**
     * Remove a processed event marker (for reprocessing)
     */
    public void clearProcessed(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            return;
        }

        try {
            String key = KEY_PREFIX + eventId;
            redisTemplate.delete(key);
            log.debug("Cleared processed marker for event: {}", eventId);
        } catch (Exception e) {
            log.warn("Failed to clear processed marker {}: {}", eventId, e.getMessage());
        }
    }
}

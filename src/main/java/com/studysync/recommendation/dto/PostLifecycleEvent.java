package com.studysync.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for post lifecycle events from Laravel backend.
 * These events are published when posts are created, updated, or deleted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostLifecycleEvent {

    public enum EventType {
        POST_CREATED,
        POST_UPDATED,
        POST_DELETED,
        POST_PUBLISHED,
        POST_UNPUBLISHED
    }

    private String eventType;
    private Long postId;
    private Long authorId;
    private LocalDateTime timestamp;
    private Map<String, Object> postData;
    private String eventId;

    /**
     * Get the event type as enum
     */
    public EventType getEventTypeEnum() {
        try {
            return EventType.valueOf(eventType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Extract title from post data
     */
    public String getTitle() {
        if (postData == null)
            return null;
        Object title = postData.get("title");
        return title != null ? title.toString() : null;
    }

    /**
     * Extract category from post data
     */
    public String getCategory() {
        if (postData == null)
            return null;
        Object category = postData.get("category");
        return category != null ? category.toString() : null;
    }

    /**
     * Check if this is a creation event
     */
    public boolean isCreationEvent() {
        return "POST_CREATED".equals(eventType) || "POST_PUBLISHED".equals(eventType);
    }

    /**
     * Check if this is a deletion event
     */
    public boolean isDeletionEvent() {
        return "POST_DELETED".equals(eventType) || "POST_UNPUBLISHED".equals(eventType);
    }
}

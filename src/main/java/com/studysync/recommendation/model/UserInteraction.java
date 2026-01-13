package com.studysync.recommendation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_interactions", indexes = {
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_post_id", columnList = "postId"),
        @Index(name = "idx_interaction_type", columnList = "interactionType"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InteractionType interactionType;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    public enum InteractionType {
        LIKE,
        UNLIKE,
        COMMENT,
        SHARE,
        VIEW,
        BOOKMARK,
        CLICK
    }
}

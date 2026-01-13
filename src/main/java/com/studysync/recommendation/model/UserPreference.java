package com.studysync.recommendation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences", indexes = {
        @Index(name = "idx_user_preference_user_id", columnList = "userId"),
        @Index(name = "idx_user_preference_category", columnList = "category")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String category; // e.g., "programming", "mathematics", "physics"

    @Column(nullable = false)
    private Double preferenceScore; // 0.0 to 1.0

    @Column(nullable = false)
    private Integer interactionCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    private void updateTimestamp() {
        lastUpdated = LocalDateTime.now();
    }
}

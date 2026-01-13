package com.studysync.recommendation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_scores", indexes = {
        @Index(name = "idx_post_score_post_id", columnList = "postId"),
        @Index(name = "idx_post_score_updated", columnList = "lastUpdated")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long postId;

    @Column(nullable = false)
    private Double totalScore;

    @Column(nullable = false)
    private Integer likeCount;

    @Column(nullable = false)
    private Integer commentCount;

    @Column(nullable = false)
    private Integer shareCount;

    @Column(nullable = false)
    private Integer viewCount;

    @Column(nullable = false)
    private Integer bookmarkCount;

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

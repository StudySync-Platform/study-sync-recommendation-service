package com.studysync.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRecommendationEvent {

    private Long userId;
    private List<RecommendedPost> recommendations;
    private String algorithm;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedPost {
        private Long postId;
        private Double score;
        private String reason; // Why this post was recommended
        private List<String> categories;
    }
}

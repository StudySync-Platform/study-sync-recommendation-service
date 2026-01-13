package com.studysync.recommendation.controller;

import com.studysync.recommendation.dto.PostRecommendationEvent;
import com.studysync.recommendation.model.PostScore;
import com.studysync.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Post recommendation endpoints")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get personalized recommendations", description = "Returns a list of recommended posts for the specified user")
    public ResponseEntity<List<PostRecommendationEvent.RecommendedPost>> getRecommendations(
            @PathVariable Long userId) {
        List<PostRecommendationEvent.RecommendedPost> recommendations = recommendationService
                .getRecommendationsForUser(userId);
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/user/{userId}/generate")
    @Operation(summary = "Trigger recommendation generation", description = "Asynchronously generates and publishes recommendations for a user")
    public ResponseEntity<String> generateRecommendations(@PathVariable Long userId) {
        recommendationService.generateAndPublishRecommendations(userId);
        return ResponseEntity.accepted().body("Recommendation generation started for user: " + userId);
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending posts", description = "Returns a list of currently trending posts based on recent engagement")
    public ResponseEntity<List<PostScore>> getTrendingPosts(
            @RequestParam(defaultValue = "10") int limit) {
        List<PostScore> trendingPosts = recommendationService.getTrendingPosts(limit);
        return ResponseEntity.ok(trendingPosts);
    }
}

package com.studysync.recommendation.controller;

import com.studysync.recommendation.dto.InteractionRequest;
import com.studysync.recommendation.model.UserInteraction;
import com.studysync.recommendation.service.InteractionService;
import com.studysync.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
@Tag(name = "Interactions", description = "User interaction management endpoints")
public class InteractionController {

    private final InteractionService interactionService;
    private final RecommendationService recommendationService;

    @PostMapping
    @Operation(summary = "Record a user interaction", description = "Records a user interaction with a post (like, comment, share, view, etc.)")
    public ResponseEntity<UserInteraction> createInteraction(@Valid @RequestBody InteractionRequest request) {
        UserInteraction interaction = interactionService.createInteraction(
                request.getUserId(),
                request.getPostId(),
                request.getInteractionType(),
                request.getCategory(),
                request.getMetadata());

        // Trigger recommendation generation asynchronously
        recommendationService.generateAndPublishRecommendations(request.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED).body(interaction);
    }

    @GetMapping("/user/{userId}/stats")
    @Operation(summary = "Get user interaction statistics", description = "Returns aggregated statistics about a user's interactions")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable Long userId) {
        Map<String, Object> stats = recommendationService.getUserInteractionStats(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/post/{postId}/stats")
    @Operation(summary = "Get post engagement statistics", description = "Returns aggregated engagement statistics for a specific post")
    public ResponseEntity<Map<String, Object>> getPostStats(@PathVariable Long postId) {
        Map<String, Object> stats = recommendationService.getPostEngagementStats(postId);
        return ResponseEntity.ok(stats);
    }
}

package com.studysync.recommendation.service;

import com.studysync.recommendation.dto.PostRecommendationEvent;
import com.studysync.recommendation.kafka.EventProducer;
import com.studysync.recommendation.model.PostScore;
import com.studysync.recommendation.model.UserInteraction;
import com.studysync.recommendation.model.UserPreference;
import com.studysync.recommendation.repository.PostScoreRepository;
import com.studysync.recommendation.repository.UserInteractionRepository;
import com.studysync.recommendation.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final UserInteractionRepository interactionRepository;
    private final PostScoreRepository postScoreRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final EventProducer eventProducer;

    @Value("${app.recommendation.time-decay-factor}")
    private Double timeDecayFactor;

    @Value("${app.recommendation.max-recommendations}")
    private Integer maxRecommendations;

    @Cacheable(value = "recommendations", key = "#userId")
    public List<PostRecommendationEvent.RecommendedPost> getRecommendationsForUser(Long userId) {
        log.info("Generating recommendations for user {}", userId);

        // Get user preferences
        List<UserPreference> preferences = userPreferenceRepository.findTopPreferencesByUser(userId);

        // Get posts the user has already interacted with
        List<Long> interactedPostIds = interactionRepository.findInteractedPostIdsByUserAndTypes(
                userId,
                Arrays.asList(UserInteraction.InteractionType.LIKE,
                        UserInteraction.InteractionType.VIEW,
                        UserInteraction.InteractionType.COMMENT));

        // Get top scoring posts
        List<PostScore> topPosts = postScoreRepository.findTopScoringPosts(
                PageRequest.of(0, maxRecommendations * 2));

        // Filter out already interacted posts and calculate personalized scores
        List<PostRecommendationEvent.RecommendedPost> recommendations = topPosts.stream()
                .filter(post -> !interactedPostIds.contains(post.getPostId()))
                .map(post -> calculatePersonalizedScore(post, preferences))
                .sorted(Comparator.comparingDouble(PostRecommendationEvent.RecommendedPost::getScore).reversed())
                .limit(maxRecommendations)
                .collect(Collectors.toList());

        log.info("Generated {} recommendations for user {}", recommendations.size(), userId);
        return recommendations;
    }

    @Async
    public void generateAndPublishRecommendations(Long userId) {
        try {
            List<PostRecommendationEvent.RecommendedPost> recommendations = getRecommendationsForUser(userId);

            PostRecommendationEvent event = PostRecommendationEvent.builder()
                    .userId(userId)
                    .recommendations(recommendations)
                    .algorithm("collaborative-content-hybrid")
                    .generatedAt(LocalDateTime.now())
                    .build();

            eventProducer.sendRecommendationEvent(event);
        } catch (Exception e) {
            log.error("Error generating recommendations for user {}", userId, e);
        }
    }

    private PostRecommendationEvent.RecommendedPost calculatePersonalizedScore(
            PostScore postScore,
            List<UserPreference> userPreferences) {

        double baseScore = postScore.getTotalScore();

        // Apply time decay
        long daysSinceCreated = ChronoUnit.DAYS.between(
                postScore.getCreatedAt(),
                LocalDateTime.now());
        double timeDecay = Math.pow(timeDecayFactor, daysSinceCreated);

        // Apply user preference boost (assuming metadata contains category)
        double preferenceBoost = 1.0;
        String reason = "Popular post";
        List<String> categories = new ArrayList<>();

        // In a real implementation, you would fetch post categories from a post service
        // For now, we'll use a simplified version

        double finalScore = baseScore * timeDecay * preferenceBoost;

        return PostRecommendationEvent.RecommendedPost.builder()
                .postId(postScore.getPostId())
                .score(finalScore)
                .reason(reason)
                .categories(categories)
                .build();
    }

    public Map<String, Object> getUserInteractionStats(Long userId) {
        List<UserInteraction> interactions = interactionRepository.findByUserId(userId);

        Map<String, Long> interactionCounts = interactions.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getInteractionType().name(),
                        Collectors.counting()));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalInteractions", interactions.size());
        stats.put("interactionBreakdown", interactionCounts);
        stats.put("lastInteraction", interactions.isEmpty() ? null
                : interactions.stream()
                        .map(UserInteraction::getTimestamp)
                        .max(LocalDateTime::compareTo)
                        .orElse(null));

        return stats;
    }

    public Map<String, Object> getPostEngagementStats(Long postId) {
        Optional<PostScore> postScoreOpt = postScoreRepository.findByPostId(postId);

        if (postScoreOpt.isEmpty()) {
            return Map.of("postId", postId, "engagement", "No data available");
        }

        PostScore postScore = postScoreOpt.get();
        Map<String, Object> stats = new HashMap<>();
        stats.put("postId", postId);
        stats.put("totalScore", postScore.getTotalScore());
        stats.put("likes", postScore.getLikeCount());
        stats.put("comments", postScore.getCommentCount());
        stats.put("shares", postScore.getShareCount());
        stats.put("views", postScore.getViewCount());
        stats.put("bookmarks", postScore.getBookmarkCount());
        stats.put("lastUpdated", postScore.getLastUpdated());

        return stats;
    }

    public List<PostScore> getTrendingPosts(int limit) {
        // Get recent posts with high engagement
        List<PostScore> topPosts = postScoreRepository.findTopScoringPosts(
                PageRequest.of(0, limit));

        return topPosts.stream()
                .filter(post -> {
                    long daysSinceCreated = ChronoUnit.DAYS.between(
                            post.getCreatedAt(),
                            LocalDateTime.now());
                    return daysSinceCreated <= 7; // Only posts from last 7 days
                })
                .collect(Collectors.toList());
    }
}

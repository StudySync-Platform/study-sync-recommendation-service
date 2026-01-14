package com.studysync.recommendation.service;

import com.studysync.recommendation.model.PostScore;
import com.studysync.recommendation.repository.PostScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing post scores and rankings.
 * 
 * Uses a hybrid approach:
 * - PostgreSQL for persistent score storage
 * - Redis Sorted Sets for fast ranking retrieval
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostScoreService {

    private final PostScoreRepository postScoreRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // Redis keys for different ranking lists
    private static final String GLOBAL_RANKING_KEY = "post_rankings:global";
    private static final String CATEGORY_RANKING_PREFIX = "post_rankings:category:";
    private static final String AUTHOR_RANKING_PREFIX = "post_rankings:author:";
    private static final String TRENDING_KEY = "post_rankings:trending";

    /**
     * Initialize a new post score when a post is created
     */
    @Transactional
    public void initializePostScore(Long postId, Long authorId, String category) {
        // Check if already exists
        if (postScoreRepository.findByPostId(postId).isPresent()) {
            log.debug("Post score already exists for postId: {}", postId);
            return;
        }

        PostScore postScore = PostScore.builder()
                .postId(postId)
                .authorId(authorId)
                .category(category)
                .totalScore(0.0)
                .likeCount(0)
                .commentCount(0)
                .shareCount(0)
                .viewCount(0)
                .bookmarkCount(0)
                .build();

        postScoreRepository.save(postScore);
        log.info("Initialized post score for postId: {}", postId);
    }

    /**
     * Update post metadata (e.g., when category changes)
     */
    @Transactional
    public void updatePostMetadata(Long postId, String category) {
        postScoreRepository.findByPostId(postId).ifPresent(postScore -> {
            String oldCategory = postScore.getCategory();
            postScore.setCategory(category);
            postScoreRepository.save(postScore);

            // Update Redis rankings if category changed
            if (oldCategory != null && !oldCategory.equals(category)) {
                removeFromCategoryRanking(postId, oldCategory);
                updateCategoryRanking(postId, postScore.getTotalScore(), category);
            }

            log.debug("Updated metadata for postId: {}", postId);
        });
    }

    /**
     * Remove post score when a post is deleted
     */
    @Transactional
    public void removePostScore(Long postId) {
        postScoreRepository.findByPostId(postId).ifPresent(postScore -> {
            // Remove from all Redis rankings
            removeFromAllRankings(postId, postScore);

            // Delete from database
            postScoreRepository.delete(postScore);
            log.info("Removed post score for postId: {}", postId);
        });
    }

    /**
     * Update the total score and sync to Redis rankings
     */
    @Transactional
    public void updateTotalScore(Long postId, Double newScore) {
        postScoreRepository.findByPostId(postId).ifPresent(postScore -> {
            postScore.setTotalScore(newScore);
            postScoreRepository.save(postScore);

            // Sync to Redis rankings
            syncToRedisRankings(postScore);

            log.debug("Updated total score for postId {}: {}", postId, newScore);
        });
    }

    /**
     * Get top N posts globally
     */
    public List<Long> getTopPosts(int limit) {
        try {
            Set<String> postIds = redisTemplate.opsForZSet()
                    .reverseRange(GLOBAL_RANKING_KEY, 0, limit - 1);

            if (postIds == null || postIds.isEmpty()) {
                // Fallback to database
                return postScoreRepository.findTopByTotalScore(limit);
            }

            return postIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting top posts from Redis, falling back to DB", e);
            return postScoreRepository.findTopByTotalScore(limit);
        }
    }

    /**
     * Get top N posts by category
     */
    public List<Long> getTopPostsByCategory(String category, int limit) {
        try {
            String key = CATEGORY_RANKING_PREFIX + category;
            Set<String> postIds = redisTemplate.opsForZSet()
                    .reverseRange(key, 0, limit - 1);

            if (postIds == null || postIds.isEmpty()) {
                return postScoreRepository.findTopByCategory(category, limit);
            }

            return postIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting top posts by category from Redis", e);
            return postScoreRepository.findTopByCategory(category, limit);
        }
    }

    /**
     * Get top N posts by author
     */
    public List<Long> getTopPostsByAuthor(Long authorId, int limit) {
        try {
            String key = AUTHOR_RANKING_PREFIX + authorId;
            Set<String> postIds = redisTemplate.opsForZSet()
                    .reverseRange(key, 0, limit - 1);

            if (postIds == null || postIds.isEmpty()) {
                return postScoreRepository.findTopByAuthor(authorId, limit);
            }

            return postIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting top posts by author from Redis", e);
            return postScoreRepository.findTopByAuthor(authorId, limit);
        }
    }

    /**
     * Get trending posts (high velocity of interactions recently)
     */
    public List<Long> getTrendingPosts(int limit) {
        try {
            Set<String> postIds = redisTemplate.opsForZSet()
                    .reverseRange(TRENDING_KEY, 0, limit - 1);

            if (postIds == null || postIds.isEmpty()) {
                return postScoreRepository.findTopByTotalScore(limit);
            }

            return postIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting trending posts from Redis", e);
            return postScoreRepository.findTopByTotalScore(limit);
        }
    }

    /**
     * Increment trending score (called on each interaction)
     */
    public void incrementTrendingScore(Long postId, Double increment) {
        try {
            redisTemplate.opsForZSet()
                    .incrementScore(TRENDING_KEY, postId.toString(), increment);
        } catch (Exception e) {
            log.warn("Failed to increment trending score for post {}", postId, e);
        }
    }

    /**
     * Sync post score to all Redis rankings
     */
    private void syncToRedisRankings(PostScore postScore) {
        try {
            String postIdStr = postScore.getPostId().toString();
            Double score = postScore.getTotalScore();

            // Global ranking
            redisTemplate.opsForZSet().add(GLOBAL_RANKING_KEY, postIdStr, score);

            // Category ranking
            if (postScore.getCategory() != null) {
                updateCategoryRanking(postScore.getPostId(), score, postScore.getCategory());
            }

            // Author ranking
            if (postScore.getAuthorId() != null) {
                String authorKey = AUTHOR_RANKING_PREFIX + postScore.getAuthorId();
                redisTemplate.opsForZSet().add(authorKey, postIdStr, score);
            }

        } catch (Exception e) {
            log.warn("Failed to sync post {} to Redis rankings", postScore.getPostId(), e);
        }
    }

    private void updateCategoryRanking(Long postId, Double score, String category) {
        try {
            String key = CATEGORY_RANKING_PREFIX + category;
            redisTemplate.opsForZSet().add(key, postId.toString(), score);
        } catch (Exception e) {
            log.warn("Failed to update category ranking for post {}", postId, e);
        }
    }

    private void removeFromCategoryRanking(Long postId, String category) {
        try {
            String key = CATEGORY_RANKING_PREFIX + category;
            redisTemplate.opsForZSet().remove(key, postId.toString());
        } catch (Exception e) {
            log.warn("Failed to remove from category ranking for post {}", postId, e);
        }
    }

    private void removeFromAllRankings(Long postId, PostScore postScore) {
        try {
            String postIdStr = postId.toString();

            redisTemplate.opsForZSet().remove(GLOBAL_RANKING_KEY, postIdStr);
            redisTemplate.opsForZSet().remove(TRENDING_KEY, postIdStr);

            if (postScore.getCategory() != null) {
                removeFromCategoryRanking(postId, postScore.getCategory());
            }

            if (postScore.getAuthorId() != null) {
                String authorKey = AUTHOR_RANKING_PREFIX + postScore.getAuthorId();
                redisTemplate.opsForZSet().remove(authorKey, postIdStr);
            }
        } catch (Exception e) {
            log.warn("Failed to remove post {} from rankings", postId, e);
        }
    }

    /**
     * Rebuild Redis rankings from database (for maintenance/recovery)
     */
    @Transactional(readOnly = true)
    public void rebuildRedisRankings() {
        log.info("Starting Redis rankings rebuild...");

        // Clear existing rankings
        redisTemplate.delete(GLOBAL_RANKING_KEY);
        redisTemplate.delete(TRENDING_KEY);

        // Rebuild from database
        List<PostScore> allScores = postScoreRepository.findAll();
        for (PostScore score : allScores) {
            syncToRedisRankings(score);
        }

        log.info("Redis rankings rebuild completed. Synced {} posts.", allScores.size());
    }
}

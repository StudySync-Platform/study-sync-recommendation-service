package com.studysync.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studysync.recommendation.dto.UserInteractionEvent;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionService {

    private final UserInteractionRepository interactionRepository;
    private final PostScoreRepository postScoreRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final EventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @Value("${app.recommendation.like-weight}")
    private Double likeWeight;

    @Value("${app.recommendation.comment-weight}")
    private Double commentWeight;

    @Value("${app.recommendation.share-weight}")
    private Double shareWeight;

    @Value("${app.recommendation.view-weight}")
    private Double viewWeight;

    @Transactional
    public UserInteraction createInteraction(Long userId, Long postId,
            UserInteraction.InteractionType type,
            String category, String metadata) {
        // Save interaction
        UserInteraction interaction = UserInteraction.builder()
                .userId(userId)
                .postId(postId)
                .interactionType(type)
                .metadata(metadata)
                .build();

        interaction = interactionRepository.save(interaction);
        log.info("Saved interaction: {}", interaction.getId());

        // Update post score
        updatePostScore(postId, type);

        // Update user preferences if category is provided
        if (category != null && !category.isEmpty()) {
            updateUserPreference(userId, category, type);
        }

        // Publish event to Kafka
        publishInteractionEvent(interaction, category);

        return interaction;
    }

    @Transactional
    public void processInteractionEvent(UserInteractionEvent event) {
        try {
            String metadataJson = null;
            if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
                metadataJson = objectMapper.writeValueAsString(event.getMetadata());
            }

            UserInteraction interaction = UserInteraction.builder()
                    .userId(event.getUserId())
                    .postId(event.getPostId())
                    .interactionType(event.getInteractionType())
                    .metadata(metadataJson)
                    .build();

            interactionRepository.save(interaction);

            // Update post score
            updatePostScore(event.getPostId(), event.getInteractionType());

            // Update user preferences if category is in metadata
            if (event.getMetadata() != null && event.getMetadata().containsKey("category")) {
                String category = event.getMetadata().get("category").toString();
                updateUserPreference(event.getUserId(), category, event.getInteractionType());
            }

        } catch (JsonProcessingException e) {
            log.error("Error processing interaction event metadata", e);
            throw new RuntimeException("Failed to process interaction event", e);
        }
    }

    private void updatePostScore(Long postId, UserInteraction.InteractionType type) {
        PostScore postScore = postScoreRepository.findByPostId(postId)
                .orElse(PostScore.builder()
                        .postId(postId)
                        .totalScore(0.0)
                        .likeCount(0)
                        .commentCount(0)
                        .shareCount(0)
                        .viewCount(0)
                        .bookmarkCount(0)
                        .build());

        // Update counts
        switch (type) {
            case LIKE:
                postScore.setLikeCount(postScore.getLikeCount() + 1);
                break;
            case COMMENT:
                postScore.setCommentCount(postScore.getCommentCount() + 1);
                break;
            case SHARE:
                postScore.setShareCount(postScore.getShareCount() + 1);
                break;
            case VIEW:
                postScore.setViewCount(postScore.getViewCount() + 1);
                break;
            case BOOKMARK:
                postScore.setBookmarkCount(postScore.getBookmarkCount() + 1);
                break;
            case UNLIKE:
                postScore.setLikeCount(Math.max(0, postScore.getLikeCount() - 1));
                break;
        }

        // Calculate total score
        double totalScore = (postScore.getLikeCount() * likeWeight) +
                (postScore.getCommentCount() * commentWeight) +
                (postScore.getShareCount() * shareWeight) +
                (postScore.getViewCount() * viewWeight) +
                (postScore.getBookmarkCount() * shareWeight);

        postScore.setTotalScore(totalScore);
        postScoreRepository.save(postScore);

        log.debug("Updated post score for postId {}: total={}", postId, totalScore);
    }

    private void updateUserPreference(Long userId, String category, UserInteraction.InteractionType type) {
        UserPreference preference = userPreferenceRepository
                .findByUserIdAndCategory(userId, category)
                .orElse(UserPreference.builder()
                        .userId(userId)
                        .category(category)
                        .preferenceScore(0.0)
                        .interactionCount(0)
                        .build());

        // Increment interaction count
        preference.setInteractionCount(preference.getInteractionCount() + 1);

        // Update preference score (weighted by interaction type)
        double increment = getPreferenceIncrement(type);
        double newScore = Math.min(1.0, preference.getPreferenceScore() + increment);
        preference.setPreferenceScore(newScore);

        userPreferenceRepository.save(preference);

        log.debug("Updated user preference: userId={}, category={}, score={}",
                userId, category, newScore);
    }

    private double getPreferenceIncrement(UserInteraction.InteractionType type) {
        return switch (type) {
            case LIKE -> 0.05;
            case COMMENT -> 0.10;
            case SHARE -> 0.15;
            case VIEW -> 0.01;
            case BOOKMARK -> 0.12;
            case UNLIKE -> -0.03;
            default -> 0.0;
        };
    }

    private void publishInteractionEvent(UserInteraction interaction, String category) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            if (category != null) {
                metadata.put("category", category);
            }
            if (interaction.getMetadata() != null) {
                metadata.put("raw", interaction.getMetadata());
            }

            UserInteractionEvent event = UserInteractionEvent.builder()
                    .userId(interaction.getUserId())
                    .postId(interaction.getPostId())
                    .interactionType(interaction.getInteractionType())
                    .timestamp(interaction.getTimestamp())
                    .metadata(metadata)
                    .build();

            eventProducer.sendInteractionEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish interaction event", e);
        }
    }
}

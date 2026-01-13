package com.studysync.recommendation.repository;

import com.studysync.recommendation.model.UserInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    List<UserInteraction> findByUserId(Long userId);

    List<UserInteraction> findByPostId(Long postId);

    List<UserInteraction> findByUserIdAndInteractionType(Long userId, UserInteraction.InteractionType interactionType);

    @Query("SELECT ui FROM UserInteraction ui WHERE ui.userId = :userId AND ui.timestamp >= :since")
    List<UserInteraction> findRecentInteractionsByUser(@Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    @Query("SELECT ui FROM UserInteraction ui WHERE ui.postId = :postId AND ui.timestamp >= :since")
    List<UserInteraction> findRecentInteractionsByPost(@Param("postId") Long postId,
            @Param("since") LocalDateTime since);

    @Query("SELECT DISTINCT ui.postId FROM UserInteraction ui WHERE ui.userId = :userId AND ui.interactionType IN :types")
    List<Long> findInteractedPostIdsByUserAndTypes(@Param("userId") Long userId,
            @Param("types") List<UserInteraction.InteractionType> types);

    Long countByPostIdAndInteractionType(Long postId, UserInteraction.InteractionType interactionType);
}

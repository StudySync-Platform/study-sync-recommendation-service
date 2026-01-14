package com.studysync.recommendation.repository;

import com.studysync.recommendation.model.PostScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostScoreRepository extends JpaRepository<PostScore, Long> {

    Optional<PostScore> findByPostId(Long postId);

    @Query("SELECT ps FROM PostScore ps ORDER BY ps.totalScore DESC")
    List<PostScore> findTopScoringPosts(Pageable pageable);

    @Query("SELECT ps FROM PostScore ps WHERE ps.postId IN :postIds")
    List<PostScore> findByPostIds(List<Long> postIds);

    void deleteByPostId(Long postId);

    /**
     * Find top N posts by total score (for global ranking)
     */
    @Query("SELECT ps.postId FROM PostScore ps ORDER BY ps.totalScore DESC LIMIT :limit")
    List<Long> findTopByTotalScore(@org.springframework.data.repository.query.Param("limit") int limit);

    /**
     * Find top N posts by category
     */
    @Query("SELECT ps.postId FROM PostScore ps WHERE ps.category = :category ORDER BY ps.totalScore DESC LIMIT :limit")
    List<Long> findTopByCategory(
            @org.springframework.data.repository.query.Param("category") String category,
            @org.springframework.data.repository.query.Param("limit") int limit);

    /**
     * Find top N posts by author
     */
    @Query("SELECT ps.postId FROM PostScore ps WHERE ps.authorId = :authorId ORDER BY ps.totalScore DESC LIMIT :limit")
    List<Long> findTopByAuthor(
            @org.springframework.data.repository.query.Param("authorId") Long authorId,
            @org.springframework.data.repository.query.Param("limit") int limit);

    /**
     * Find posts by category
     */
    List<PostScore> findByCategory(String category);

    /**
     * Find posts by author
     */
    List<PostScore> findByAuthorId(Long authorId);
}

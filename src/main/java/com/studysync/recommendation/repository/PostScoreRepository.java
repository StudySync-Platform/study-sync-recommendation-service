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
}

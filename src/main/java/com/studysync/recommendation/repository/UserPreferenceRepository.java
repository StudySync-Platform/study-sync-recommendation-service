package com.studysync.recommendation.repository;

import com.studysync.recommendation.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    List<UserPreference> findByUserId(Long userId);

    Optional<UserPreference> findByUserIdAndCategory(Long userId, String category);

    @Query("SELECT up FROM UserPreference up WHERE up.userId = :userId ORDER BY up.preferenceScore DESC")
    List<UserPreference> findTopPreferencesByUser(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}

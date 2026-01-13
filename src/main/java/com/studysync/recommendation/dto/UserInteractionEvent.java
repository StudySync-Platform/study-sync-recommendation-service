package com.studysync.recommendation.dto;

import com.studysync.recommendation.model.UserInteraction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInteractionEvent {

    private Long userId;
    private Long postId;
    private UserInteraction.InteractionType interactionType;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}

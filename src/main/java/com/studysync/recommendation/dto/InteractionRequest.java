package com.studysync.recommendation.dto;

import com.studysync.recommendation.model.UserInteraction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionRequest {

    private Long userId;
    private Long postId;
    private UserInteraction.InteractionType interactionType;
    private String category; // Post category
    private String metadata; // Additional JSON data
}

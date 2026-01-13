package com.studysync.recommendation.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.user-interaction}")
    private String userInteractionTopic;

    @Value("${app.kafka.topics.post-recommendation}")
    private String postRecommendationTopic;

    @Bean
    public NewTopic userInteractionTopic() {
        return TopicBuilder.name(userInteractionTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic postRecommendationTopic() {
        return TopicBuilder.name(postRecommendationTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

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

    @Value("${app.kafka.topics.post-lifecycle:post-lifecycle-events}")
    private String postLifecycleTopic;

    @Value("${app.kafka.topics.dlq:recommendation-dlq}")
    private String dlqTopic;

    @Bean
    public NewTopic userInteractionTopic() {
        return TopicBuilder.name(userInteractionTopic)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .build();
    }

    @Bean
    public NewTopic postRecommendationTopic() {
        return TopicBuilder.name(postRecommendationTopic)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .build();
    }

    @Bean
    public NewTopic postLifecycleTopic() {
        return TopicBuilder.name(postLifecycleTopic)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(dlqTopic)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days for manual inspection
                .build();
    }
}

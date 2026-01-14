package com.studysync.recommendation.grpc.client;

import com.studysync.recommendation.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * gRPC Client for fetching post and user details from Laravel backend.
 * 
 * This client is used to hydrate recommendation data with full post/user
 * information when serving recommendations to users.
 */
@Component
@Slf4j
public class GrpcPostClient {

    @Value("${grpc.client.laravel.host:localhost}")
    private String laravelGrpcHost;

    @Value("${grpc.client.laravel.port:6001}")
    private int laravelGrpcPort;

    @Value("${grpc.client.laravel.timeout-seconds:5}")
    private int timeoutSeconds;

    private ManagedChannel channel;
    private PostDetailServiceGrpc.PostDetailServiceBlockingStub blockingStub;

    @PostConstruct
    public void init() {
        log.info("Initializing gRPC client for Laravel backend at {}:{}", laravelGrpcHost, laravelGrpcPort);

        channel = ManagedChannelBuilder.forAddress(laravelGrpcHost, laravelGrpcPort)
                .usePlaintext() // Use TLS in production
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .build();

        blockingStub = PostDetailServiceGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                log.info("gRPC channel shutdown completed");
            } catch (InterruptedException e) {
                log.warn("gRPC channel shutdown interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Fetch details for a single post from Laravel backend
     */
    public Optional<PostResponse> getPostInfo(Long postId) {
        try {
            PostRequest request = PostRequest.newBuilder()
                    .setPostId(postId)
                    .build();

            PostResponse response = blockingStub
                    .withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS)
                    .getPostInfo(request);

            if (response.getId() == 0) {
                log.debug("Post not found: {}", postId);
                return Optional.empty();
            }

            log.debug("Retrieved post info: id={}, title={}", response.getId(), response.getTitle());
            return Optional.of(response);

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for post {}: {} - {}",
                    postId, e.getStatus().getCode(), e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error fetching post {}", postId, e);
            return Optional.empty();
        }
    }

    /**
     * Fetch details for multiple posts in batch from Laravel backend
     */
    public List<PostResponse> getBatchPostInfo(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            BatchPostRequest request = BatchPostRequest.newBuilder()
                    .addAllPostIds(postIds)
                    .build();

            BatchPostResponse response = blockingStub
                    .withDeadlineAfter(timeoutSeconds * 2, TimeUnit.SECONDS) // Double timeout for batch
                    .getBatchPostInfo(request);

            log.debug("Retrieved {} posts in batch (requested: {})",
                    response.getPostsCount(), postIds.size());
            return response.getPostsList();

        } catch (StatusRuntimeException e) {
            log.error("gRPC batch call failed: {} - {}",
                    e.getStatus().getCode(), e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error in batch post fetch", e);
            return Collections.emptyList();
        }
    }

    /**
     * Fetch user profile for personalization from Laravel backend
     */
    public Optional<UserProfileResponse> getUserProfile(Long userId) {
        try {
            UserProfileRequest request = UserProfileRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            UserProfileResponse response = blockingStub
                    .withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS)
                    .getUserProfile(request);

            if (response.getId() == 0) {
                log.debug("User not found: {}", userId);
                return Optional.empty();
            }

            log.debug("Retrieved user profile: id={}, name={}", response.getId(), response.getName());
            return Optional.of(response);

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for user {}: {} - {}",
                    userId, e.getStatus().getCode(), e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error fetching user {}", userId, e);
            return Optional.empty();
        }
    }

    /**
     * Check if the gRPC connection is healthy
     */
    public boolean isHealthy() {
        try {
            // Try to fetch a non-existent post as a health check
            PostRequest request = PostRequest.newBuilder()
                    .setPostId(-1L)
                    .build();

            blockingStub
                    .withDeadlineAfter(2, TimeUnit.SECONDS)
                    .getPostInfo(request);

            return true;
        } catch (StatusRuntimeException e) {
            // UNAVAILABLE means connection issue, other errors mean service is reachable
            return !e.getStatus().getCode().name().equals("UNAVAILABLE");
        } catch (Exception e) {
            return false;
        }
    }
}

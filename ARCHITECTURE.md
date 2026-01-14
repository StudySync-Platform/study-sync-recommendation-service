# Post Recommendation Microservice Architecture

## 📚 Overview

This document describes the event-driven microservice architecture for the Post Recommendation System, which collects user interaction events from the Laravel backend and processes them to generate personalized post recommendations.

## 🏗️ Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           STUDY-SYNC RECOMMENDATION ARCHITECTURE                        │
└─────────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────┐          ┌─────────────────────────────┐
│     LARAVEL BACKEND         │          │    SPRING BOOT SERVICE      │
│   (study-sync-backend)      │          │ (study-sync-recommendation) │
│                             │          │                             │
│  ┌───────────────────────┐  │          │  ┌───────────────────────┐  │
│  │   Post Controller     │  │          │  │   Kafka Consumers     │  │
│  │   User Controller     │  │          │  │  ┌─────────────────┐  │  │
│  └───────────┬───────────┘  │          │  │  │ Interaction     │  │  │
│              │              │          │  │  │ Consumer        │  │  │
│              ▼              │          │  │  └─────────────────┘  │  │
│  ┌───────────────────────┐  │          │  │  ┌─────────────────┐  │  │
│  │   Event Dispatcher    │  │          │  │  │ Lifecycle       │  │  │
│  │  ┌─────────────────┐  │  │          │  │  │ Consumer        │  │  │
│  │  │PostLiked        │  │  │          │  │  └─────────────────┘  │  │
│  │  │PostViewed       │  │  │          │  └───────────────────────┘  │
│  │  │PostCommented    │  │  │          │              │              │
│  │  │PostShared       │  │  │          │              ▼              │
│  │  │PostBookmarked   │  │  │          │  ┌───────────────────────┐  │
│  │  └─────────────────┘  │  │          │  │   Scoring Service     │  │
│  └───────────┬───────────┘  │          │  │  ┌─────────────────┐  │  │
│              │              │          │  │  │ Calculate       │  │  │
│              ▼              │          │  │  │ Post Scores     │  │  │
│  ┌───────────────────────┐  │          │  │  └─────────────────┘  │  │
│  │   Kafka Producer      │  │          │  │  ┌─────────────────┐  │  │
│  │  (InteractionProducer)│──┼──────────┼──┼──│ Update Redis    │  │  │
│  └───────────────────────┘  │  Kafka   │  │  │ Rankings        │  │  │
│              │              │ Events   │  │  └─────────────────┘  │  │
│              │              │          │  └───────────────────────┘  │
│  ┌───────────────────────┐  │          │              │              │
│  │   gRPC Server (6001)  │◄─┼──────────┼──────────────┘              │
│  │  (PostDetailService)  │  │  gRPC    │              ▼              │
│  │  ┌─────────────────┐  │  │ Requests │  ┌───────────────────────┐  │
│  │  │GetPostInfo      │  │  │          │  │   gRPC Client         │  │
│  │  │GetBatchPostInfo │  │  │          │  │  (GrpcPostClient)     │  │
│  │  │GetUserProfile   │  │  │          │  └───────────────────────┘  │
│  │  └─────────────────┘  │  │          │                             │
│  └───────────────────────┘  │          │  ┌───────────────────────┐  │
│                             │          │  │   REST API            │  │
└─────────────────────────────┘          │  │  /api/recommendations │  │
                                         │  └───────────────────────┘  │
                                         └─────────────────────────────┘
                                                       │
              ┌────────────────────────────────────────┼────────────────────────────────────┐
              │                                        │                                    │
              ▼                                        ▼                                    ▼
┌──────────────────────┐              ┌──────────────────────┐              ┌──────────────────────┐
│      Apache Kafka    │              │      PostgreSQL      │              │        Redis         │
│                      │              │                      │              │                      │
│  Topics:             │              │  Tables:             │              │  Sorted Sets:        │
│  ├─ user-interaction │              │  ├─ user_interactions│              │  ├─ post_rankings:   │
│  │   -events         │              │  ├─ post_scores      │              │  │   global           │
│  ├─ post-lifecycle   │              │  └─ user_preferences │              │  ├─ post_rankings:   │
│  │   -events         │              │                      │              │  │   category:{name}  │
│  ├─ post-recommendation│            │                      │              │  └─ post_rankings:   │
│  │   -events         │              │                      │              │      trending        │
│  └─ recommendation-dlq│             │                      │              │                      │
│     (Dead Letter Q)  │              │                      │              │  Hash:               │
└──────────────────────┘              └──────────────────────┘              │  └─ processed_event: │
                                                                           │      {eventId}       │
                                                                           └──────────────────────┘
```

## 📁 Folder Structure

### Laravel Backend (`study-sync-backend`)

```
app/
├── Events/
│   ├── PostLiked.php              # Event fired when user likes a post
│   ├── PostViewed.php             # Event fired when user views a post
│   ├── PostCommented.php          # Event fired when user comments
│   ├── PostShared.php             # Event fired when user shares
│   └── PostBookmarked.php         # Event fired when user bookmarks
│
├── Grpc/
│   ├── Generated/                 # Compiled protobuf classes
│   │   └── Recommendation/
│   │       ├── PostRequest.php
│   │       ├── PostResponse.php
│   │       ├── BatchPostRequest.php
│   │       ├── BatchPostResponse.php
│   │       ├── UserProfileRequest.php
│   │       └── UserProfileResponse.php
│   │
│   └── Services/
│       └── PostDetailServiceHandler.php  # gRPC service implementation
│
├── Kafka/
│   ├── DTOs/
│   │   ├── InteractionEvent.php   # DTO for user interactions
│   │   └── PostLifecycleEvent.php # DTO for post CRUD events
│   │
│   └── Producers/
│       └── InteractionProducer.php # Kafka producer with retry logic
│
├── Listeners/
│   └── PublishInteractionToKafka.php # Listens to events, publishes to Kafka
│
└── ...

config/
└── kafka.php                      # Kafka configuration

proto/
├── matchmaking.proto              # Existing proto
├── study.proto                    # Existing proto
└── recommendation.proto           # NEW: Post recommendation service proto
```

### Spring Boot Recommendation Service (`study-sync-recommendation-service`)

```
src/
├── main/
│   ├── java/com/studysync/recommendation/
│   │   ├── RecommendationApplication.java
│   │   │
│   │   ├── config/
│   │   │   ├── KafkaTopicConfig.java      # Kafka topic definitions
│   │   │   ├── RedisConfig.java           # Redis configuration
│   │   │   └── OpenApiConfig.java         # Swagger documentation
│   │   │
│   │   ├── controller/
│   │   │   ├── RecommendationController.java
│   │   │   └── InteractionController.java
│   │   │
│   │   ├── dto/
│   │   │   ├── UserInteractionEvent.java  # Kafka consumer DTO
│   │   │   ├── PostLifecycleEvent.java    # Post lifecycle DTO
│   │   │   ├── PostRecommendationEvent.java
│   │   │   └── InteractionRequest.java
│   │   │
│   │   ├── grpc/
│   │   │   └── client/
│   │   │       └── GrpcPostClient.java    # gRPC client for Laravel
│   │   │
│   │   ├── kafka/
│   │   │   ├── EventProducer.java         # Kafka producer
│   │   │   ├── UserInteractionConsumer.java  # Basic consumer
│   │   │   └── consumer/
│   │   │       ├── EnhancedEventConsumer.java  # Consumer with DLQ
│   │   │       └── IdempotencyService.java     # Deduplication
│   │   │
│   │   ├── model/
│   │   │   ├── UserInteraction.java       # JPA entity
│   │   │   ├── PostScore.java             # Post ranking scores
│   │   │   └── UserPreference.java        # User preferences
│   │   │
│   │   ├── repository/
│   │   │   ├── UserInteractionRepository.java
│   │   │   ├── PostScoreRepository.java
│   │   │   └── UserPreferenceRepository.java
│   │   │
│   │   └── service/
│   │       ├── InteractionService.java    # Process interactions
│   │       ├── RecommendationService.java # Generate recommendations
│   │       └── PostScoreService.java      # Score management
│   │
│   ├── proto/
│   │   └── recommendation.proto           # gRPC service definition
│   │
│   └── resources/
│       └── application.yml                # Configuration
│
└── test/
    └── ...
```

## 📨 Kafka Topics & Message Schemas

### Topic: `user-interaction-events`

**Purpose:** Captures all user interactions with posts.

```json
{
  "userId": 12345,
  "postId": 67890,
  "interactionType": "LIKE",
  "timestamp": "2024-03-20T10:30:00Z",
  "metadata": {
    "category": "technology",
    "eventId": "evt_abc123"
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `userId` | Long | ID of the user performing the action |
| `postId` | Long | ID of the post being interacted with |
| `interactionType` | Enum | LIKE, UNLIKE, VIEW, COMMENT, SHARE, BOOKMARK, CLICK |
| `timestamp` | ISO8601 | When the interaction occurred |
| `metadata` | Object | Optional additional context |

### Topic: `post-lifecycle-events`

**Purpose:** Tracks post creation, updates, and deletion.

```json
{
  "eventType": "POST_CREATED",
  "postId": 67890,
  "authorId": 12345,
  "timestamp": "2024-03-20T10:00:00Z",
  "postData": {
    "title": "Introduction to Kafka",
    "category": "technology",
    "tags": ["kafka", "microservices"]
  },
  "eventId": "evt_xyz789"
}
```

### Topic: `recommendation-dlq`

**Purpose:** Dead Letter Queue for failed message processing.

## 🔌 gRPC Interface

### Service Definition (`recommendation.proto`)

```protobuf
service PostDetailService {
  rpc GetPostInfo (PostRequest) returns (PostResponse);
  rpc GetBatchPostInfo (BatchPostRequest) returns (BatchPostResponse);
  rpc GetUserProfile (UserProfileRequest) returns (UserProfileResponse);
}
```

### Use Cases:

1. **Hydrating Recommendations:** When the recommendation service generates a list of recommended post IDs, it uses gRPC to fetch full post details (title, author, etc.) before returning to the client.

2. **User Profile for Personalization:** Fetches user interests and followed categories to personalize recommendations.

## 🔄 Event Flow

```
1. User Action (Frontend)
        │
        ▼
2. Laravel Controller (e.g., LikeController@store)
        │
        ▼
3. Dispatch Event (PostLiked::class)
        │
        ▼
4. PublishInteractionToKafka Listener
        │
        ▼
5. InteractionProducer::publishLike()
        │
        ▼
6. Kafka Topic: user-interaction-events
        │
        ▼
7. Spring Boot: EnhancedEventConsumer
        │
        ├── Check idempotency (Redis)
        │
        ▼
8. InteractionService::processInteractionEvent()
        │
        ├── Save to PostgreSQL (user_interactions)
        ├── Update PostScore
        └── Update UserPreference
        │
        ▼
9. PostScoreService::syncToRedisRankings()
        │
        ▼
10. Redis Sorted Sets Updated
        │
        ▼
11. /api/recommendations Endpoint (Fast Redis Retrieval)
        │
        ├── Fetch post IDs from Redis
        └── Hydrate with gRPC (GetBatchPostInfo)
        │
        ▼
12. Return Recommendations to User
```

## ✅ Best Practices Implemented

### 1. **Idempotency**
- Each event includes a unique `eventId`
- `IdempotencyService` tracks processed events in Redis
- TTL of 24 hours prevents memory bloat

### 2. **Retry & Error Handling**
- Kafka producer retries: 3 attempts with exponential backoff
- Consumer uses manual acknowledgment
- Failed messages routed to DLQ (`recommendation-dlq`)

### 3. **Dead Letter Queue (DLQ)**
- Separate consumer for DLQ inspection
- 30-day retention for manual review
- Can be reprocessed after fixing issues

### 4. **Caching Strategy (Hybrid DB + Redis)**
- **PostgreSQL:** Permanent storage of scores and interactions
- **Redis Sorted Sets:** Fast O(log N) retrieval of top posts
- Automatic sync on every score update

### 5. **gRPC for Low Latency**
- Used only for synchronous data fetching
- Batch operations reduce round trips
- Connection pooling with keep-alive

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker (for Kafka, PostgreSQL, Redis)
- PHP 8.2+ with gRPC extension

### 1. Start Infrastructure

```bash
cd study-sync-recommendation-service
docker-compose up -d
```

### 2. Compile Proto Files (Spring Boot)

```bash
cd study-sync-recommendation-service
mvn compile
```

### 3. Compile Proto Files (Laravel)

```bash
cd study-sync-backend
protoc --php_out=app/Grpc/Generated \
       --grpc_out=app/Grpc/Generated \
       --plugin=protoc-gen-grpc=/usr/local/bin/grpc_php_plugin \
       proto/recommendation.proto
```

### 4. Install Laravel Kafka Package

```bash
composer require mateusjunges/laravel-kafka
php artisan vendor:publish --tag=laravel-kafka-config
```

### 5. Run Services

```bash
# Terminal 1: Laravel Backend
cd study-sync-backend
php artisan serve

# Terminal 2: Spring Boot Service
cd study-sync-recommendation-service
mvn spring-boot:run

# Terminal 3: Laravel Queue Worker
cd study-sync-backend
php artisan queue:work
```

## 📊 Monitoring

- **Spring Boot Actuator:** `http://localhost:8084/actuator/health`
- **Prometheus Metrics:** `http://localhost:8084/actuator/prometheus`
- **Swagger UI:** `http://localhost:8084/swagger-ui.html`

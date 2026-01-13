# Project Structure

```
study-sync-recommendation-service/
├── pom.xml                                 # Maven configuration with dependencies
├── docker-compose.yml                       # Docker setup for Kafka, PostgreSQL, Redis
├── start.sh                                 # Quick start script
├── README.md                                # Comprehensive documentation
├── QUICKSTART.md                            # Quick reference guide
├── postman-collection.json                  # API testing collection
│
├── src/main/
│   ├── java/com/studysync/recommendation/
│   │   ├── RecommendationApplication.java   # Main Spring Boot application
│   │   │
│   │   ├── config/                          # Configuration classes
│   │   │   ├── KafkaTopicConfig.java        # Kafka topic auto-creation
│   │   │   ├── OpenApiConfig.java           # Swagger/OpenAPI setup
│   │   │   └── RedisConfig.java             # Redis caching configuration
│   │   │
│   │   ├── controller/                      # REST API endpoints
│   │   │   ├── InteractionController.java   # User interaction endpoints
│   │   │   └── RecommendationController.java # Recommendation endpoints
│   │   │
│   │   ├── dto/                             # Data Transfer Objects
│   │   │   ├── InteractionRequest.java      # API request DTO
│   │   │   ├── UserInteractionEvent.java    # Kafka event DTO
│   │   │   └── PostRecommendationEvent.java # Recommendation event DTO
│   │   │
│   │   ├── kafka/                           # Kafka messaging
│   │   │   ├── EventProducer.java           # Publishes events to Kafka
│   │   │   └── UserInteractionConsumer.java # Consumes interaction events
│   │   │
│   │   ├── model/                           # JPA entity models
│   │   │   ├── UserInteraction.java         # User interaction entity
│   │   │   ├── PostScore.java               # Post engagement scores
│   │   │   └── UserPreference.java          # User interest profiles
│   │   │
│   │   ├── repository/                      # Database access layer
│   │   │   ├── UserInteractionRepository.java
│   │   │   ├── PostScoreRepository.java
│   │   │   └── UserPreferenceRepository.java
│   │   │
│   │   └── service/                         # Business logic layer
│   │       ├── InteractionService.java      # Interaction processing & scoring
│   │       └── RecommendationService.java   # Recommendation algorithm
│   │
│   └── resources/
│       └── application.yml                  # Application configuration
│
└── src/test/
    ├── java/com/studysync/recommendation/
    │   └── RecommendationApplicationTests.java
    └── resources/
        └── application-test.yml             # Test configuration

```

## Key Components

### 📊 Entities (PostgreSQL)
- **UserInteraction**: All user interactions (likes, comments, shares, views, bookmarks)
- **PostScore**: Aggregated engagement scores per post
- **UserPreference**: User interest profiles by category

### 🔄 Kafka Topics
- **user-interaction-events**: Real-time interaction stream
- **post-recommendation-events**: Generated recommendations

### 🎯 Core Services
- **InteractionService**: Processes interactions, updates scores, tracks preferences
- **RecommendationService**: Hybrid algorithm (popularity + time decay + personalization)

### 🌐 REST API
- `/api/v1/interactions` - Record and query interactions
- `/api/v1/recommendations` - Get personalized & trending recommendations

### ⚡ Infrastructure
- **Kafka**: Event streaming and async processing
- **PostgreSQL**: Primary data store
- **Redis**: Caching layer (1-hour TTL)
- **Docker Compose**: Local development environment

## Data Flow

```
User Action
    ↓
REST API (InteractionController)
    ↓
InteractionService
    ├→ Save to PostgreSQL (UserInteraction)
    ├→ Update PostScore
    ├→ Update UserPreference
    └→ Publish to Kafka (user-interaction-events)
         ↓
    Kafka Consumer (UserInteractionConsumer)
         ↓
    Process & Store
         ↓
RecommendationService
    ├→ Calculate scores (popularity + time decay + preferences)
    ├→ Cache in Redis
    └→ Publish to Kafka (post-recommendation-events)
         ↓
    External Services (Frontend, Notifications, etc.)
```

## Algorithm Overview

**Scoring Formula:**
```
PostScore = (likes × 1.0) + (comments × 2.0) + (shares × 3.0) + 
            (views × 0.5) + (bookmarks × 3.0)

PersonalizedScore = PostScore × TimeDecay × PreferenceBoost

TimeDecay = 0.95 ^ DaysSinceCreation
```

**Features:**
- Weighted engagement metrics
- Time decay for freshness
- User preference matching
- Collaborative filtering ready
- Excludes already-seen posts

## Configuration

Key settings in `application.yml`:
```yaml
app:
  recommendation:
    like-weight: 1.0
    comment-weight: 2.0
    share-weight: 3.0
    view-weight: 0.5
    time-decay-factor: 0.95
    max-recommendations: 20
```

## Dependencies

- Spring Boot 3.3.0
- Spring Kafka
- Spring Data JPA
- PostgreSQL Driver
- Spring Data Redis
- Lombok
- SpringDoc OpenAPI
- H2 (testing)

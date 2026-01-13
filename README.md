# StudySync Recommendation Service

A Spring Boot microservice that handles user interaction events and generates intelligent post recommendations using a hybrid algorithm combining collaborative filtering and content-based approaches.

## Features

- **User Interaction Tracking**: Records and processes user interactions (likes, comments, shares, views, bookmarks)
- **Real-time Event Processing**: Kafka-based event streaming for scalable interaction handling
- **Intelligent Recommendations**: Hybrid recommendation algorithm with:
  - Popularity-based scoring
  - Time decay for recency
  - User preference matching
  - Collaborative filtering
- **Caching**: Redis-based caching for improved performance
- **RESTful API**: Comprehensive REST endpoints for interactions and recommendations
- **OpenAPI Documentation**: Swagger UI for API exploration
- **Async Processing**: Non-blocking recommendation generation
- **Analytics**: User and post engagement statistics

## Technology Stack

- **Java 17**
- **Spring Boot 3.3.0**
- **Apache Kafka**: Event streaming
- **PostgreSQL**: Primary database
- **Redis**: Caching layer
- **Maven**: Dependency management
- **Lombok**: Boilerplate reduction
- **SpringDoc OpenAPI**: API documentation

## Architecture

```
┌─────────────┐      ┌──────────────────┐      ┌─────────────┐
│   Client    │─────▶│  REST API        │─────▶│   Kafka     │
│ Application │      │  Controller      │      │   Topics    │
└─────────────┘      └──────────────────┘      └─────────────┘
                              │                        │
                              ▼                        ▼
                     ┌──────────────────┐    ┌─────────────────┐
                     │  Service Layer   │    │ Kafka Consumer  │
                     │  - Interaction   │    │                 │
                     │  - Recommendation│    │                 │
                     └──────────────────┘    └─────────────────┘
                              │                        │
                     ┌────────┴────────┐              │
                     ▼                 ▼              ▼
              ┌─────────────┐   ┌──────────┐  ┌─────────────┐
              │ PostgreSQL  │   │  Redis   │  │  Algorithm  │
              │             │   │  Cache   │  │   Engine    │
              └─────────────┘   └──────────┘  └─────────────┘
```

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose (for dependencies)

## Getting Started

### 1. Start Dependencies

Start Kafka, PostgreSQL, Redis, and Kafka UI using Docker Compose:

```bash
docker-compose up -d
```

This will start:
- Kafka on `localhost:9092`
- Zookeeper on `localhost:2181`
- PostgreSQL on `localhost:5433`
- Redis on `localhost:6379`
- Kafka UI on `http://localhost:8080`

### 2. Build the Application

```bash
mvn clean install
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The service will start on `http://localhost:8084`

## API Endpoints

### Interactions

#### Record User Interaction
```http
POST /api/v1/interactions
Content-Type: application/json

{
  "userId": 1,
  "postId": 100,
  "interactionType": "LIKE",
  "category": "programming",
  "metadata": "{\"source\": \"mobile\"}"
}
```

Interaction types: `LIKE`, `UNLIKE`, `COMMENT`, `SHARE`, `VIEW`, `BOOKMARK`, `CLICK`

#### Get User Interaction Statistics
```http
GET /api/v1/interactions/user/{userId}/stats
```

#### Get Post Engagement Statistics
```http
GET /api/v1/interactions/post/{postId}/stats
```

### Recommendations

#### Get Personalized Recommendations
```http
GET /api/v1/recommendations/user/{userId}
```

Returns a list of recommended posts with scores and reasons.

#### Trigger Recommendation Generation
```http
POST /api/v1/recommendations/user/{userId}/generate
```

Asynchronously generates and publishes recommendations via Kafka.

#### Get Trending Posts
```http
GET /api/v1/recommendations/trending?limit=10
```

Returns currently trending posts (last 7 days).

## API Documentation

Access the Swagger UI at: `http://localhost:8084/swagger-ui.html`

## Kafka Topics

### user-interaction-events
- **Purpose**: User interaction events
- **Partitions**: 3
- **Producer**: InteractionService, EventProducer
- **Consumer**: UserInteractionConsumer

### post-recommendation-events
- **Purpose**: Generated recommendations
- **Partitions**: 3
- **Producer**: RecommendationService
- **Consumer**: External services (frontend, notification service, etc.)

## Recommendation Algorithm

The service uses a hybrid recommendation algorithm:

1. **Popularity Scoring**: Weighted engagement metrics
   - Likes: 1.0x
   - Comments: 2.0x
   - Shares: 3.0x
   - Views: 0.5x
   - Bookmarks: 3.0x

2. **Time Decay**: Recent posts are favored
   - Decay factor: 0.95 per day
   - Prevents stale content from dominating

3. **User Preferences**: Category-based personalization
   - Tracks user interests per category
   - Boosts relevant content

4. **Filtering**: Excludes previously interacted posts

## Configuration

Key configuration properties in `application.yml`:

```yaml
app:
  kafka:
    topics:
      user-interaction: user-interaction-events
      post-recommendation: post-recommendation-events
  
  recommendation:
    like-weight: 1.0
    comment-weight: 2.0
    share-weight: 3.0
    view-weight: 0.5
    time-decay-factor: 0.95
    max-recommendations: 20
```

## Database Schema

### user_interactions
- Stores all user interaction events
- Indexed by userId, postId, interactionType, timestamp

### post_scores
- Aggregated engagement scores per post
- Updated in real-time as interactions occur

### user_preferences
- User interest profiles per category
- Used for personalized recommendations

## Monitoring

### Kafka UI
Access at `http://localhost:8080` to monitor:
- Topic messages
- Consumer group lag
- Broker health

### Application Logs
Set log level in `application.yml`:
```yaml
logging:
  level:
    com.studysync.recommendation: DEBUG
```

## Testing

Run tests:
```bash
mvn test
```

## Performance Considerations

- **Caching**: Recommendations are cached in Redis (1-hour TTL)
- **Async Processing**: Recommendation generation is non-blocking
- **Batch Operations**: Bulk updates for efficiency
- **Indexing**: Database indexes on frequently queried columns

## Future Enhancements

- Machine learning model integration (collaborative filtering)
- A/B testing framework for algorithm improvements
- Real-time recommendation updates via WebSocket
- Integration with content analysis service
- User clustering for improved recommendations
- Multi-armed bandit for exploration/exploitation

## Troubleshooting

### Kafka Connection Issues
```bash
# Check Kafka status
docker-compose ps

# View Kafka logs
docker-compose logs -f kafka
```

### Database Connection Issues
```bash
# Check PostgreSQL status
docker-compose ps postgres

# Access PostgreSQL
docker exec -it studysync-postgres-recommendation psql -U postgres -d studysync_recommendation
```

### Redis Connection Issues
```bash
# Check Redis status
docker-compose ps redis

# Test Redis
docker exec -it studysync-redis redis-cli ping
```

## License

Apache 2.0

## Contact

StudySync Team - support@studysync.com

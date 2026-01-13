# StudySync Recommendation Service - Setup Summary

## ✅ What Has Been Created

A complete Spring Boot microservice for handling user interaction events and generating intelligent post recommendations.

## 📁 Project Location
`/Users/apple/Documents/study-sync/study-sync-recommendation-service/`

## 🎯 Features Implemented

### 1. User Interaction Tracking
- **7 Interaction Types**: LIKE, UNLIKE, COMMENT, SHARE, VIEW, BOOKMARK, CLICK
- Real-time event recording via REST API
- Automatic score calculation and aggregation
- User preference tracking by category

### 2. Recommendation Engine
- **Hybrid Algorithm**:
  - Popularity-based scoring
  - Time decay for content freshness
  - User preference personalization
  - Filters out already-seen content
- Cached results for performance (Redis, 1-hour TTL)
- Async recommendation generation
- Trending posts detection

### 3. Event Streaming (Kafka)
- **Producer**: Publishes interaction and recommendation events
- **Consumer**: Processes interaction events asynchronously
- **Topics**:
  - `user-interaction-events`
  - `post-recommendation-events`

### 4. REST API
- Full CRUD operations for interactions
- Personalized recommendations endpoint
- Trending posts endpoint
- User and post analytics
- OpenAPI/Swagger documentation

### 5. Infrastructure
- PostgreSQL for data persistence
- Redis for caching
- Kafka for event streaming
- Docker Compose for local development

## 📦 Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Framework | Spring Boot 3.3.0 | Core application |
| Language | Java 17 | Programming language |
| Messaging | Apache Kafka | Event streaming |
| Database | PostgreSQL | Primary storage |
| Cache | Redis | Performance optimization |
| Build Tool | Maven | Dependency management |
| API Docs | SpringDoc OpenAPI | Documentation |
| Containers | Docker Compose | Local environment |

## 🚀 Quick Start

### 1. Start Dependencies
```bash
cd /Users/apple/Documents/study-sync/study-sync-recommendation-service
docker-compose up -d
```

### 2. Build & Run
```bash
./start.sh
```

OR manually:
```bash
mvn clean install
mvn spring-boot:run
```

### 3. Access Points
- **API**: http://localhost:8084
- **Swagger UI**: http://localhost:8084/swagger-ui.html
- **Kafka UI**: http://localhost:8080

## 📊 Database Schema

### Tables Created Automatically
1. **user_interactions** - All interaction events
2. **post_scores** - Aggregated engagement metrics
3. **user_preferences** - User interest profiles

## 🔌 API Endpoints

### Interactions
```
POST   /api/v1/interactions              - Record interaction
GET    /api/v1/interactions/user/{id}/stats  - User stats
GET    /api/v1/interactions/post/{id}/stats  - Post stats
```

### Recommendations
```
GET    /api/v1/recommendations/user/{id}        - Get recommendations
POST   /api/v1/recommendations/user/{id}/generate - Trigger generation
GET    /api/v1/recommendations/trending         - Get trending posts
```

## 📝 Example Usage

### Record a Like
```bash
curl -X POST http://localhost:8084/api/v1/interactions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "postId": 100,
    "interactionType": "LIKE",
    "category": "programming"
  }'
```

### Get Recommendations
```bash
curl http://localhost:8084/api/v1/recommendations/user/1
```

## 🧪 Testing

### Run Tests
```bash
mvn test
```

### Postman Collection
Import `postman-collection.json` into Postman for comprehensive API testing.

## 📚 Documentation Files

| File | Description |
|------|-------------|
| `README.md` | Comprehensive documentation |
| `QUICKSTART.md` | Quick reference guide |
| `PROJECT_STRUCTURE.md` | Architecture details |
| `postman-collection.json` | API test collection |

## ⚙️ Configuration

### Recommendation Algorithm Settings
Edit `src/main/resources/application.yml`:

```yaml
app:
  recommendation:
    like-weight: 1.0          # Weight for likes
    comment-weight: 2.0       # Weight for comments
    share-weight: 3.0         # Weight for shares
    view-weight: 0.5          # Weight for views
    time-decay-factor: 0.95   # Freshness decay
    max-recommendations: 20   # Maximum results
```

### Database Connection
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/studysync_recommendation
    username: postgres
    password: postgres
```

### Kafka Configuration
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
```

## 🔍 Monitoring

### Kafka UI
Visit http://localhost:8080 to monitor:
- Message flow
- Consumer lag
- Topic statistics

### Database
```bash
docker exec -it studysync-postgres-recommendation psql -U postgres -d studysync_recommendation
```

## 🛠️ Troubleshooting

### Port Conflicts
If port 8084 is in use, change it in `application.yml`:
```yaml
server:
  port: 8085
```

### Kafka Not Starting
```bash
docker-compose logs kafka
docker-compose restart kafka
```

### Clean Slate
```bash
docker-compose down -v  # Remove all data
docker-compose up -d    # Fresh start
```

## 🎨 Architecture Highlights

### Service Layer Architecture
```
Controller → Service → Repository → Database
     ↓
  Kafka Producer
     ↓
  Kafka Consumer
     ↓
  Service (Processing)
```

### Scoring Algorithm
```
Base Score = Σ (interaction_count × weight)
Time Decay = 0.95 ^ days_since_creation
Preference Boost = Match with user interests
Final Score = Base Score × Time Decay × Preference Boost
```

## 🔄 Integration with Other Services

### Publishing Events
Other services can consume from:
- `user-interaction-events` - For analytics, notifications
- `post-recommendation-events` - For frontend, email campaigns

### Consuming Events
This service can consume from other topics (future enhancement):
- Post metadata updates
- User profile changes
- Content categorization

## 🚀 Next Steps

1. **Start the service**: `./start.sh`
2. **Explore the API**: Visit Swagger UI
3. **Test with Postman**: Import the collection
4. **Monitor Kafka**: Check the Kafka UI
5. **Integration**: Connect with your frontend/backend

## 📞 Support

For issues or questions:
- Check the documentation files
- Review Kafka UI for message flow
- Check application logs
- Verify Docker containers are running

## 🎯 Success Criteria

✅ Kafka, PostgreSQL, Redis running  
✅ Application starts on port 8084  
✅ Swagger UI accessible  
✅ Can record interactions  
✅ Can get recommendations  
✅ Events flow through Kafka  

---

**Created**: January 2026  
**Version**: 1.0.0  
**Status**: Ready for Development & Testing

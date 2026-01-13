# Quick Start Guide

## 1. Start the Service

### Using Docker Compose (Recommended)
```bash
# Start dependencies
docker-compose up -d

# Build and run the application
./start.sh
```

### Manual Setup
```bash
# Start Kafka, PostgreSQL, Redis
docker-compose up -d

# Build
mvn clean install

# Run
mvn spring-boot:run
```

## 2. Verify Setup

- **Application**: http://localhost:8084
- **Swagger UI**: http://localhost:8084/swagger-ui.html
- **Kafka UI**: http://localhost:8080
- **Health Check**: http://localhost:8084/actuator/health (if actuator is enabled)

## 3. Test the API

### Record a User Interaction
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

### Get Trending Posts
```bash
curl http://localhost:8084/api/v1/recommendations/trending?limit=5
```

## 4. Monitor Kafka

Visit http://localhost:8080 to view:
- Topic messages
- Consumer groups
- Message flow

## 5. Database Access

```bash
# Connect to PostgreSQL
docker exec -it studysync-postgres-recommendation psql -U postgres -d studysync_recommendation

# List tables
\dt

# View user interactions
SELECT * FROM user_interactions;

# View post scores
SELECT * FROM post_scores ORDER BY total_score DESC LIMIT 10;
```

## 6. Stop the Service

```bash
# Stop Spring Boot application
# Press Ctrl+C in the terminal

# Stop Docker containers
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

## Common Issues

### Port Already in Use
If port 8084 is in use, change it in `application.yml`:
```yaml
server:
  port: 8085
```

### Kafka Connection Failed
Ensure Kafka is running:
```bash
docker-compose ps kafka
```

### Database Connection Failed
Check PostgreSQL:
```bash
docker-compose logs postgres
```

## Environment Variables

Override configuration using environment variables:
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://your-host:5432/your-db
export SPRING_KAFKA_BOOTSTRAP_SERVERS=your-kafka:9092
mvn spring-boot:run
```

## Production Deployment

For production, create `application-prod.yml`:
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
  
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}
  
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
```

Run with production profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

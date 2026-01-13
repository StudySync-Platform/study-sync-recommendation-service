#!/bin/bash

echo "🚀 Starting StudySync Recommendation Service..."
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

echo "📦 Starting dependencies (Kafka, PostgreSQL, Redis)..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be ready..."
sleep 10

echo ""
echo "🔨 Building the application..."
mvn clean package -DskipTests

echo ""
echo "✅ Starting the Spring Boot application..."
mvn spring-boot:run

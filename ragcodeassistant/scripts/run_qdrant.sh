#!/bin/bash

# Script to start Qdrant vector database locally using Docker

echo "Starting Qdrant vector database..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker first."
    exit 1
fi

# Stop existing Qdrant container if running
echo "Stopping existing Qdrant container (if any)..."
docker stop qdrant-rag 2>/dev/null || true
docker rm qdrant-rag 2>/dev/null || true

# Create data directory for persistence
mkdir -p ./data/qdrant

# Start Qdrant container
echo "Starting new Qdrant container..."
docker run -d \
    --name qdrant-rag \
    -p 6333:6333 \
    -p 6334:6334 \
    -v $(pwd)/data/qdrant:/qdrant/storage \
    qdrant/qdrant:latest

# Wait for Qdrant to be ready
echo "Waiting for Qdrant to be ready..."
sleep 5

# Check if Qdrant is responding
if curl -s http://localhost:6333/health > /dev/null; then
    echo "✅ Qdrant is running successfully!"
    echo "   - REST API: http://localhost:6333"
    echo "   - Web UI: http://localhost:6333/dashboard"
    echo "   - gRPC: localhost:6334"
    echo ""
    echo "To stop Qdrant: docker stop qdrant-rag"
    echo "To view logs: docker logs qdrant-rag"
else
    echo "❌ Qdrant failed to start properly"
    echo "Check logs with: docker logs qdrant-rag"
    exit 1
fi
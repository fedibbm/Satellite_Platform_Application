#!/bin/bash
# Script to start the GEE service with proper DNS configuration

cd "$(dirname "$0")"

echo "Starting GEE service..."

# Check if docker-compose is available
if command -v docker-compose &> /dev/null; then
    docker-compose up -d
else
    # Fallback to docker run with DNS configuration
    docker stop gee_app 2>/dev/null
    docker rm gee_app 2>/dev/null
    
    docker run -d \
      --name gee_app \
      --dns 8.8.8.8 \
      --dns 8.8.4.4 \
      -p 5000:5000 \
      -v "$(pwd)":/app \
      --restart unless-stopped \
      gee_app_with_cache_logic:latest
fi

echo "GEE service started successfully!"
echo "Checking logs..."
sleep 3
docker logs gee_app --tail 20

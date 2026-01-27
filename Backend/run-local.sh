#!/bin/bash
# Load environment variables and run Spring Boot locally

cd "$(dirname "$0")"

# Export all variables from .env file
set -a
source .env
set +a

# Run Spring Boot
./mvnw spring-boot:run

#!/bin/bash

# Satellite Platform Application - Setup Script
# This script helps you set up and run the entire application

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Satellite Platform Application Setup${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

if ! command_exists docker; then
    echo -e "${RED}❌ Docker is not installed. Please install Docker first.${NC}"
    exit 1
else
    echo -e "${GREEN}✓ Docker is installed${NC}"
fi

if ! command_exists docker-compose; then
    echo -e "${RED}❌ Docker Compose is not installed. Please install Docker Compose first.${NC}"
    exit 1
else
    echo -e "${GREEN}✓ Docker Compose is installed${NC}"
fi

# Check if Docker daemon is running
if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}❌ Docker daemon is not running. Please start Docker first.${NC}"
    exit 1
else
    echo -e "${GREEN}✓ Docker daemon is running${NC}"
fi

echo ""

# Check for required files
echo -e "${YELLOW}Checking configuration files...${NC}"

if [ ! -f "Backend/.env" ]; then
    echo -e "${YELLOW}⚠ Backend/.env not found. Creating from example...${NC}"
    if [ -f "Backend/.env.example" ]; then
        cp Backend/.env.example Backend/.env
        echo -e "${GREEN}✓ Backend/.env created${NC}"
    else
        echo -e "${RED}❌ Backend/.env.example not found${NC}"
    fi
else
    echo -e "${GREEN}✓ Backend/.env exists${NC}"
fi

if [ ! -f "gee_app_with_cache_logic/.env" ]; then
    echo -e "${YELLOW}⚠ gee_app_with_cache_logic/.env not found. Creating from example...${NC}"
    if [ -f "gee_app_with_cache_logic/.env.example" ]; then
        cp gee_app_with_cache_logic/.env.example gee_app_with_cache_logic/.env
        echo -e "${GREEN}✓ gee_app_with_cache_logic/.env created${NC}"
    else
        echo -e "${RED}❌ gee_app_with_cache_logic/.env.example not found${NC}"
    fi
else
    echo -e "${GREEN}✓ gee_app_with_cache_logic/.env exists${NC}"
fi

if [ ! -f "FrontEnd/.env.local" ]; then
    echo -e "${YELLOW}⚠ FrontEnd/.env.local not found. Creating from example...${NC}"
    if [ -f "FrontEnd/.env.example" ]; then
        cp FrontEnd/.env.example FrontEnd/.env.local
        echo -e "${GREEN}✓ FrontEnd/.env.local created${NC}"
    else
        echo -e "${RED}❌ FrontEnd/.env.example not found${NC}"
    fi
else
    echo -e "${GREEN}✓ FrontEnd/.env.local exists${NC}"
fi

# Check for GEE service account key
echo ""
echo -e "${YELLOW}Checking Google Earth Engine service account...${NC}"
if [ ! -f "gee_app_with_cache_logic/satellite-platform-application-f7154aa5ce46.json" ]; then
    echo -e "${RED}❌ GEE service account key not found!${NC}"
    echo -e "${YELLOW}   Please place your service account JSON file at:${NC}"
    echo -e "${YELLOW}   gee_app_with_cache_logic/satellite-platform-application-f7154aa5ce46.json${NC}"
    echo ""
    read -p "Do you want to continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo -e "${GREEN}✓ GEE service account key found${NC}"
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Starting Application${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Ask user what they want to do
echo "What would you like to do?"
echo "1) Start all services (docker-compose up -d)"
echo "2) Build and start all services (docker-compose up -d --build)"
echo "3) Stop all services (docker-compose down)"
echo "4) Stop and remove volumes (docker-compose down -v)"
echo "5) View logs (docker-compose logs -f)"
echo "6) Check service status (docker-compose ps)"
echo "7) Exit"
echo ""
read -p "Enter your choice [1-7]: " choice

case $choice in
    1)
        echo -e "${GREEN}Starting all services...${NC}"
        docker-compose up -d
        ;;
    2)
        echo -e "${GREEN}Building and starting all services...${NC}"
        docker-compose up -d --build
        ;;
    3)
        echo -e "${YELLOW}Stopping all services...${NC}"
        docker-compose down
        ;;
    4)
        echo -e "${RED}Stopping all services and removing volumes...${NC}"
        read -p "Are you sure? This will delete all data! (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker-compose down -v
        fi
        ;;
    5)
        echo -e "${BLUE}Viewing logs (Ctrl+C to exit)...${NC}"
        docker-compose logs -f
        ;;
    6)
        echo -e "${BLUE}Service status:${NC}"
        docker-compose ps
        ;;
    7)
        echo -e "${BLUE}Exiting...${NC}"
        exit 0
        ;;
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Operation completed!${NC}"
echo -e "${GREEN}========================================${NC}\n"

if [ "$choice" -eq 1 ] || [ "$choice" -eq 2 ]; then
    echo -e "${BLUE}Application URLs:${NC}"
    echo -e "${GREEN}• Frontend:${NC}          http://localhost:3000"
    echo -e "${GREEN}• Backend API:${NC}       http://localhost:8080"
    echo -e "${GREEN}• Swagger UI:${NC}        http://localhost:8080/swagger-ui.html"
    echo -e "${GREEN}• GEE Service:${NC}       http://localhost:5000"
    echo -e "${GREEN}• Image Processing:${NC}  http://localhost:8000"
    echo -e "${GREEN}• RabbitMQ UI:${NC}       http://localhost:15672 (admin/admin123)"
    echo -e "${GREEN}• Prometheus:${NC}        http://localhost:9090"
    echo -e "${GREEN}• Grafana:${NC}           http://localhost:3001 (admin/admin123)"
    echo ""
    echo -e "${YELLOW}View logs:${NC} docker-compose logs -f"
    echo -e "${YELLOW}Stop services:${NC} docker-compose down"
fi

echo ""

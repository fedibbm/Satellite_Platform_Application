# Satellite Platform Application

A collaborative web platform for satellite imagery analysis and geospatial data processing using microservices architecture.

## 🏗️ Architecture Overview

This project consists of **4 independent microservices**:

```
┌─────────────────┐
│  Frontend       │  Next.js (Port 3000)
│  (Next.js)      │
└────────┬────────┘
         │ HTTP/REST
         ▼
┌─────────────────────────────┐
│  Backend                    │  Spring Boot (Port 8080)
│  (Spring Boot + Java 17)    │
└─────┬───────────────────┬───┘
      │                   │
      │ HTTP/REST         │ HTTP/REST
      ▼                   ▼
┌──────────────────┐   ┌─────────────────────┐
│ GEE Service      │   │ Image Processing    │
│ (Flask/Python)   │   │ (FastAPI/Python)    │
│ Port: 5000       │   │ Port: 8000          │
└──────────────────┘   └─────────────────────┘
```

**Additional Services:**
- **MongoDB** (Port 27017) - Main database
- **Redis** (Port 6379) - Caching & rate limiting
- **RabbitMQ** - Message queue (optional)
- **Prometheus** (Port 9090) - Metrics monitoring
- **Grafana** (Port 3001) - Metrics visualization

---

## 📋 Prerequisites

### Required Software
- **Java 17** (JDK)
- **Maven 3.8+**
- **Node.js 18+** and **npm**
- **Python 3.12+**
- **MongoDB 5.0+**
- **Redis 6.0+**
- **Docker & Docker Compose** (recommended)

### Accounts & API Keys
- **Google Earth Engine Account** (for GEE service)
- **Google Cloud Project** with Earth Engine API enabled
- **Service Account** with Earth Engine permissions

---

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/fedibbm/Satellite_Platform_Application.git
cd Satellite_Platform_Application

# Start all services
docker-compose up -d

# Access the application
# Frontend: http://localhost:3000
# Backend: http://localhost:8080
# GEE Service: http://localhost:5000
# Image Processing: http://localhost:8000
```

### Option 2: Manual Setup (Development)

Follow the detailed setup for each service below.

---

## 🔧 Service-by-Service Setup

## 1️⃣ Backend Service (Spring Boot)

### Prerequisites
- Java 17 JDK
- Maven 3.8+
- MongoDB running on port 27017
- Redis running on port 6379

### Environment Variables

Create a `.env` file in the `Backend/` directory or set these environment variables:

```bash
# MongoDB
MONGO_URI=mongodb://localhost:27017/satellitedb

# Redis
REDIS_URL=redis://localhost:6379

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
JWT_EXPIRATION=86400000

# Admin User (Initial Setup)
ADMIN_ENABLED=true
ADMIN_USERNAME=admin
ADMIN_EMAIL=admin@satellite.com
ADMIN_PASSWORD=Admin@123

# Python Services URLs
PYTHON_BASE_URL=http://localhost:5000

# Storage Paths
PROJECT_BASE_PATH=/path/to/project/storage
IMAGE_STORAGE_PATH=/path/to/image/storage
TEMP_STORAGE_PATH=tmp

# RabbitMQ (Optional)
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

### Installation & Running

```bash
cd Backend

# Install dependencies
./mvnw clean install

# Run the application
./mvnw spring-boot:run

# Or using Maven wrapper on Windows
mvnw.cmd spring-boot:run

# Application will start on http://localhost:8080
```

### Verify Backend is Running

```bash
curl http://localhost:8080/actuator/health
```

Expected response: `{"status":"UP"}`

---

## 2️⃣ Google Earth Engine (GEE) Service

### Prerequisites
- Python 3.12+
- Google Earth Engine Service Account
- Redis (for caching)

### Google Earth Engine Setup

#### Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the **Earth Engine API**:
   - Navigate to "APIs & Services" → "Library"
   - Search for "Earth Engine API"
   - Click "Enable"

#### Step 2: Create Service Account

1. Go to "IAM & Admin" → "Service Accounts"
2. Click "Create Service Account"
3. Fill in details:
   - **Name**: `satellite-platform-gee`
   - **Description**: Service account for Earth Engine access
4. Click "Create and Continue"
5. Grant role: **"Earth Engine Resource Admin"** or **"Earth Engine Resource Viewer"**
6. Click "Done"

#### Step 3: Generate JSON Key

1. Click on the created service account
2. Go to "Keys" tab
3. Click "Add Key" → "Create new key"
4. Select "JSON" format
5. Click "Create" - A JSON file will download automatically

#### Step 4: Register Service Account with Earth Engine

```bash
# Install Earth Engine CLI
pip install earthengine-api

# Authenticate with your Google account (one-time)
earthengine authenticate

# Register the service account
earthengine set_project your-project-id
```

Or register via [Earth Engine Code Editor](https://code.earthengine.google.com/).

### Installation & Configuration

```bash
cd gee_app_with_cache_logic

# Create virtual environment
python -m venv venv

# Activate virtual environment
# On Linux/Mac:
source venv/bin/activate
# On Windows:
venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Place your service account JSON key file
mkdir -p config
cp /path/to/your-service-account-key.json ./satellite-platform-application-f7154aa5ce46.json
```

### Environment Variables

Create a `.env` file in `gee_app_with_cache_logic/`:

```bash
# Google Earth Engine
GEE_SERVICE_ACCOUNT=your-service-account@your-project.iam.gserviceaccount.com
GEE_KEY_FILE=./satellite-platform-application-f7154aa5ce46.json

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DB=0

# Flask Configuration
FLASK_ENV=development
FLASK_DEBUG=True
```

### Running the Service

```bash
cd gee_app_with_cache_logic

# Activate virtual environment
source venv/bin/activate  # Linux/Mac
# or
venv\Scripts\activate  # Windows

# Run the Flask application
python app.py

# Service will start on http://localhost:5000
```

### Verify GEE Service is Running

```bash
curl http://localhost:5000/
```

Expected response: JSON with service information

### Using Docker (Alternative)

```bash
cd gee_app_with_cache_logic

# Build the image
docker build -t gee-service .

# Run the container
docker run -d \
  -p 5000:5000 \
  -v $(pwd)/satellite-platform-application-f7154aa5ce46.json:/app/satellite-platform-application-f7154aa5ce46.json \
  --env-file .env \
  --name gee-service \
  gee-service

# Or use docker-compose
docker-compose up -d
```

---

## 3️⃣ Image Processing Service (FastAPI)

### Prerequisites
- Python 3.13+
- GDAL (Geospatial Data Abstraction Library)

### Install GDAL (Required for Rasterio)

#### On Ubuntu/Debian:
```bash
sudo apt-get update
sudo apt-get install -y gdal-bin libgdal-dev python3-dev build-essential
```

#### On macOS:
```bash
brew install gdal
```

#### On Windows:
Download GDAL from [GISInternals](https://www.gisinternals.com/release.php)

### Installation

```bash
cd image_porcessing

# Create virtual environment
python -m venv venv

# Activate virtual environment
# On Linux/Mac:
source venv/bin/activate
# On Windows:
venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

### Running the Service

```bash
cd image_porcessing

# Activate virtual environment
source venv/bin/activate  # Linux/Mac
# or
venv\Scripts\activate  # Windows

# Run the FastAPI application
cd app
uvicorn REST_API_version2:app --host 0.0.0.0 --port 8000 --reload

# Service will start on http://localhost:8000
```

### Verify Image Processing Service

```bash
curl http://localhost:8000/
```

Expected response: Welcome message with API endpoints

### API Documentation

Once running, access the interactive API documentation:
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

### Using Docker (Alternative)

```bash
cd image_porcessing

# Build the image
docker build -t image-processing-service .

# Run the container
docker run -d \
  -p 8000:8000 \
  -v $(pwd)/output:/app/output \
  --name image-processing-service \
  image-processing-service
```

---

## 4️⃣ Frontend Service (Next.js)

### Prerequisites
- Node.js 18+
- npm or yarn

### Installation

```bash
cd FrontEnd

# Install dependencies
npm install
# or
yarn install
```

### Environment Variables

Create a `.env.local` file in `FrontEnd/`:

```bash
# API Endpoints
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_GEE_API_URL=http://localhost:5000
NEXT_PUBLIC_IMAGE_API_URL=http://localhost:8000

# Authentication
NEXT_PUBLIC_JWT_SECRET=your-jwt-secret
```

### Running the Service

```bash
cd FrontEnd

# Development mode (with hot reload)
npm run dev
# or
yarn dev

# Production build
npm run build
npm start

# Application will start on http://localhost:3000
```

### Verify Frontend

Open browser and navigate to: http://localhost:3000

---

## 🗄️ Database Setup

### MongoDB

#### Install MongoDB

**Ubuntu/Debian:**
```bash
sudo apt-get install -y mongodb-org
sudo systemctl start mongod
sudo systemctl enable mongod
```

**macOS:**
```bash
brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb-community
```

**Using Docker:**
```bash
docker run -d \
  -p 27017:27017 \
  --name mongodb \
  -v mongodb_data:/data/db \
  mongo:latest
```

#### Initialize Database

The Backend service will automatically create the database and collections on first run.

### Redis

#### Install Redis

**Ubuntu/Debian:**
```bash
sudo apt-get install -y redis-server
sudo systemctl start redis
sudo systemctl enable redis
```

**macOS:**
```bash
brew install redis
brew services start redis
```

**Using Docker:**
```bash
docker run -d \
  -p 6379:6379 \
  --name redis \
  redis:latest
```

#### Verify Redis

```bash
redis-cli ping
```

Expected response: `PONG`

---

## 📊 Monitoring Setup (Optional)

### Prometheus & Grafana

```bash
cd Backend/monitoring

# Start monitoring stack
docker-compose up -d

# Access Grafana: http://localhost:3001
# Default credentials: admin/admin

# Access Prometheus: http://localhost:9090
```

---

## 🧪 Testing the Platform

### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test@123"
  }'
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123"
  }'
```

Save the returned `accessToken` for subsequent requests.

### 3. Test GEE Service

```bash
curl -X POST http://localhost:5000/api/gee/ndvi \
  -H "Content-Type: application/json" \
  -d '{
    "region": {
      "type": "Polygon",
      "coordinates": [[[-74.0, 40.7], [-74.0, 40.8], [-73.9, 40.8], [-73.9, 40.7], [-74.0, 40.7]]]
    },
    "startDate": "2024-01-01",
    "endDate": "2024-12-31"
  }'
```

### 4. Test Image Processing Service

```bash
curl -X POST http://localhost:8000/calculate/ndvi \
  -F "file=@/path/to/satellite_image.tif" \
  -F 'metadata={"redBand":1,"nirBand":2}'
```

---

## 🐳 Docker Compose (Full Stack)

### Complete docker-compose.yml

Create a `docker-compose.yml` in the root directory:

```yaml
version: '3.8'

services:
  mongodb:
    image: mongo:latest
    container_name: mongodb
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db
    networks:
      - satellite-network

  redis:
    image: redis:latest
    container_name: redis
    ports:
      - "6379:6379"
    networks:
      - satellite-network

  backend:
    build: ./Backend
    container_name: backend
    ports:
      - "8080:8080"
    environment:
      - MONGO_URI=mongodb://mongodb:27017/satellitedb
      - REDIS_URL=redis://redis:6379
      - PYTHON_BASE_URL=http://gee-service:5000
    depends_on:
      - mongodb
      - redis
    networks:
      - satellite-network

  gee-service:
    build: ./gee_app_with_cache_logic
    container_name: gee-service
    ports:
      - "5000:5000"
    volumes:
      - ./gee_app_with_cache_logic/satellite-platform-application-f7154aa5ce46.json:/app/satellite-platform-application-f7154aa5ce46.json
    environment:
      - REDIS_HOST=redis
      - REDIS_PORT=6379
    depends_on:
      - redis
    networks:
      - satellite-network

  image-processing:
    build: ./image_porcessing
    container_name: image-processing
    ports:
      - "8000:8000"
    volumes:
      - ./image_porcessing/output:/app/output
    networks:
      - satellite-network

  frontend:
    build: ./FrontEnd
    container_name: frontend
    ports:
      - "3000:3000"
    environment:
      - NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
      - NEXT_PUBLIC_GEE_API_URL=http://localhost:5000
      - NEXT_PUBLIC_IMAGE_API_URL=http://localhost:8000
    depends_on:
      - backend
    networks:
      - satellite-network

volumes:
  mongodb_data:

networks:
  satellite-network:
    driver: bridge
```

### Start All Services

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

---

## 🔍 Troubleshooting

### Backend Issues

**MongoDB Connection Failed:**
```bash
# Check MongoDB is running
sudo systemctl status mongod

# Check connection
mongosh --eval "db.adminCommand('ping')"
```

**Redis Connection Failed:**
```bash
# Check Redis is running
redis-cli ping

# Check Redis service
sudo systemctl status redis
```

### GEE Service Issues

**Authentication Error:**
```bash
# Verify service account file exists
ls -la satellite-platform-application-*.json

# Test Earth Engine authentication
python -c "import ee; ee.Initialize(); print('Success!')"
```

**Import Error: No module named 'ee':**
```bash
# Ensure virtual environment is activated
source venv/bin/activate

# Reinstall dependencies
pip install -r requirements.txt
```

### Image Processing Issues

**GDAL Not Found:**
```bash
# Ubuntu/Debian
sudo apt-get install -y gdal-bin libgdal-dev

# Verify GDAL installation
gdalinfo --version

# Reinstall rasterio
pip install --upgrade rasterio
```

**Permission Denied on Output Directory:**
```bash
# Create output directory with correct permissions
mkdir -p image_porcessing/output
chmod 777 image_porcessing/output
```

### Frontend Issues

**Cannot Connect to Backend:**
- Verify Backend is running on port 8080
- Check CORS settings in Backend
- Verify `.env.local` has correct API URLs

**Module Not Found:**
```bash
# Clean install
rm -rf node_modules package-lock.json
npm install
```

---

## 📚 Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/)
- [Google Earth Engine Documentation](https://developers.google.com/earth-engine)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [Next.js Documentation](https://nextjs.org/docs)
- [MongoDB Documentation](https://docs.mongodb.com/)
- [Redis Documentation](https://redis.io/documentation)

---

## 👥 Contributors

- Fedi Ben Brahim - 2AINFO2
- Oussema Ben Ameur - 2AINFO2

---

## 📄 License

This project is developed as part of a Final Year Project (PFA) at ENIT (École Nationale d'Ingénieurs de Tunis).

---

## 🆘 Support

For issues and questions:
1. Check the troubleshooting section above
2. Review individual service READMEs in their respective directories
3. Contact the development team

---

**Last Updated:** December 2025

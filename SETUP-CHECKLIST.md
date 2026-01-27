# ✅ Satellite Platform Application - Setup Checklist

This checklist will guide you through the complete setup process.

## 📋 Pre-Setup Checklist

### 1. System Requirements
- [ ] Docker Engine 20.10+ installed
- [ ] Docker Compose 2.0+ installed
- [ ] At least 8GB RAM available
- [ ] At least 20GB free disk space
- [ ] Linux/macOS/Windows with WSL2

### 2. Google Earth Engine Setup
- [ ] Google Cloud Project created
- [ ] Earth Engine API enabled
- [ ] Service Account created with Earth Engine permissions
- [ ] Service Account JSON key downloaded
- [ ] Service Account registered with Earth Engine

## 🔧 Configuration Steps

### Step 1: Backend Configuration
```bash
cd Backend
cp .env.example .env
nano .env
```

**Required configurations in Backend/.env:**
- [ ] `MONGO_URI` - Set MongoDB connection (default is fine for Docker)
- [ ] `REDIS_URL` - Set Redis connection (default is fine for Docker)
- [ ] `JWT_SECRET` - **IMPORTANT:** Change to a secure random string (32+ chars)
- [ ] `JWT_EXPIRATION` - Token expiration time (default: 86400000 = 24 hours)
- [ ] `ADMIN_USERNAME` - Admin username for initial setup
- [ ] `ADMIN_EMAIL` - Admin email
- [ ] `ADMIN_PASSWORD` - **IMPORTANT:** Change default password
- [ ] `PYTHON_BASE_URL` - Image processing service URL (default is fine)
- [ ] `RABBITMQ_HOST` - RabbitMQ host (default is fine for Docker)
- [ ] `RABBITMQ_USER` - RabbitMQ username (default: admin)
- [ ] `RABBITMQ_PASS` - **IMPORTANT:** Change default password

### Step 2: GEE Service Configuration
```bash
cd gee_app_with_cache_logic
cp .env.example .env
nano .env
```

**Required configurations:**
- [ ] `GEE_SERVICE_ACCOUNT` - Your service account email
- [ ] `GEE_KEY_FILE` - Path to service account JSON key
- [ ] `GEE_PROJECT_ID` - Your Google Cloud project ID
- [ ] `REDIS_HOST` - Redis host (default is fine for Docker)
- [ ] Place service account JSON file: `satellite-platform-application-f7154aa5ce46.json`

### Step 3: Frontend Configuration
```bash
cd FrontEnd
cp .env.example .env.local
nano .env.local
```

**Required configurations:**
- [ ] `NEXT_PUBLIC_API_BASE_URL` - Backend API URL (default: http://localhost:8080)
- [ ] `NEXT_PUBLIC_GEE_API_URL` - GEE service URL (default: http://localhost:5000)
- [ ] `NEXT_PUBLIC_IMAGE_API_URL` - Image processing URL (default: http://localhost:8000)

## 🐳 Docker Setup

### Option 1: Using Setup Script (Recommended)
```bash
cd /path/to/Satellite_Platform_Application
./setup.sh
```

- [ ] Run setup script
- [ ] Choose option 2 to build and start all services
- [ ] Wait for all services to start (this may take 5-10 minutes first time)

### Option 2: Manual Docker Commands
```bash
cd /path/to/Satellite_Platform_Application

# Build and start all services
docker-compose up -d --build

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

## ✅ Verification Steps

### 1. Check All Services Are Running
```bash
docker-compose ps
```

**Expected output:** All services should show "Up" status
- [ ] mongodb - healthy
- [ ] redis - healthy
- [ ] rabbitmq - healthy
- [ ] gee-service - running
- [ ] image-processing-app - running
- [ ] backend - healthy
- [ ] frontend - running
- [ ] prometheus - running
- [ ] grafana - running

### 2. Test Service Endpoints

**Backend API:**
```bash
curl http://localhost:8080/actuator/health
```
- [ ] Returns: `{"status":"UP"}`

**GEE Service:**
```bash
curl http://localhost:5000/
```
- [ ] Returns: JSON response with service info

**Image Processing:**
```bash
curl http://localhost:8000/
```
- [ ] Returns: Welcome message

**Frontend:**
- [ ] Open http://localhost:3000 in browser
- [ ] Page loads successfully

**RabbitMQ Management:**
- [ ] Open http://localhost:15672
- [ ] Login with admin/admin123
- [ ] Dashboard displays

**Grafana:**
- [ ] Open http://localhost:3001
- [ ] Login with admin/admin123
- [ ] Dashboards available

### 3. Test User Registration & Login

**Register a test user:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test@123"
  }'
```
- [ ] Returns success response with user info

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123"
  }'
```
- [ ] Returns access token

### 4. Test Database Connections

**MongoDB:**
```bash
docker-compose exec mongodb mongosh -u admin -p admin123
```
- [ ] Successfully connects
- [ ] Run: `show dbs` - Should show satellitedb

**Redis:**
```bash
docker-compose exec redis redis-cli ping
```
- [ ] Returns: `PONG`

## 🎯 Post-Setup Tasks

### Security (Production)
- [ ] Change all default passwords
- [ ] Generate strong JWT secret (32+ characters)
- [ ] Update RabbitMQ credentials
- [ ] Update MongoDB credentials
- [ ] Update Grafana credentials
- [ ] Review and restrict CORS settings
- [ ] Enable HTTPS with reverse proxy
- [ ] Set up firewall rules

### Monitoring
- [ ] Configure Grafana datasources
- [ ] Import dashboards
- [ ] Set up alert rules
- [ ] Configure log aggregation

### Backups
- [ ] Set up MongoDB backup schedule
- [ ] Configure volume backup strategy
- [ ] Test restore procedures

## 🐛 Common Issues & Solutions

### Issue: Services fail to start
**Solution:**
```bash
# Check logs
docker-compose logs [service-name]

# Restart specific service
docker-compose restart [service-name]

# Rebuild if needed
docker-compose build --no-cache [service-name]
docker-compose up -d [service-name]
```

### Issue: Port already in use
**Solution:** Edit `docker-compose.yml` and change port mappings

### Issue: Backend can't connect to MongoDB
**Solution:** 
- Check MongoDB is healthy: `docker-compose ps`
- Verify connection string in `Backend/.env`
- Check logs: `docker-compose logs mongodb backend`

### Issue: GEE service authentication fails
**Solution:**
- Verify service account JSON file exists
- Check file permissions
- Verify service account has Earth Engine permissions
- Check `.env` has correct values

### Issue: Out of memory
**Solution:** Increase Docker memory limit in Docker Desktop settings (minimum 8GB)

## 📚 Next Steps

- [ ] Read the API documentation: http://localhost:8080/swagger-ui.html
- [ ] Explore the frontend: http://localhost:3000
- [ ] Test GEE functionality with sample coordinates
- [ ] Upload and process satellite images
- [ ] Configure monitoring dashboards
- [ ] Set up development environment

## 📞 Support

If you encounter issues:
1. Check the logs: `docker-compose logs -f`
2. Review the main [README.md](README.md)
3. Check [DOCKER-SETUP.md](DOCKER-SETUP.md) for detailed Docker info
4. Ensure all checklist items are completed

---

**Status:** [ ] Setup Incomplete  [ ] Setup Complete  [ ] Production Ready

**Date:** ___________

**Notes:**
```

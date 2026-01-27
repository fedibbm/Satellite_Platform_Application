# Satellite Platform Application - Docker Setup

## 🚀 Quick Start

### Prerequisites
- Docker Engine 20.10+
- Docker Compose 2.0+
- 8GB+ RAM available
- 20GB+ free disk space

### Setup Steps

1. **Configure Environment Variables**
   ```bash
   # Backend
   cp Backend/.env.example Backend/.env
   nano Backend/.env  # Edit with your values

   # GEE Service
   cp gee_app_with_cache_logic/.env.example gee_app_with_cache_logic/.env
   nano gee_app_with_cache_logic/.env  # Add your GEE credentials

   # Frontend
   cp FrontEnd/.env.example FrontEnd/.env.local
   ```

2. **Add Google Earth Engine Service Account Key**
   - Place your GEE service account JSON file at:
     ```
     gee_app_with_cache_logic/satellite-platform-application-f7154aa5ce46.json
     ```

3. **Start All Services**
   ```bash
   # Using the setup script (recommended)
   chmod +x setup.sh
   ./setup.sh

   # Or manually
   docker-compose up -d
   ```

## 📊 Services & Ports

| Service | Port | Description |
|---------|------|-------------|
| **Frontend** | 3000 | Next.js web application |
| **Backend** | 8080 | Spring Boot REST API |
| **GEE Service** | 5000 | Google Earth Engine service |
| **Image Processing** | 8000 | FastAPI image processing |
| **MongoDB** | 27017 | Database |
| **Redis** | 6379 | Cache & sessions |
| **RabbitMQ** | 5672, 15672 | Message broker & UI |
| **Prometheus** | 9090 | Metrics |
| **Grafana** | 3001 | Monitoring dashboards |

## 🔗 Access URLs

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **RabbitMQ Management**: http://localhost:15672 (admin/admin123)
- **Grafana**: http://localhost:3001 (admin/admin123)
- **Prometheus**: http://localhost:9090

## 🛠️ Common Commands

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f backend

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v

# Rebuild specific service
docker-compose build backend
docker-compose up -d backend

# Check service status
docker-compose ps

# Execute command in container
docker-compose exec backend bash
docker-compose exec mongodb mongosh
docker-compose exec redis redis-cli
```

## 🔧 Development Mode

For development with hot-reload:

```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up
```

## 🐛 Troubleshooting

### Services not starting
```bash
# Check logs
docker-compose logs

# Check specific service
docker-compose logs backend

# Verify health status
docker-compose ps
```

### Port conflicts
Edit `docker-compose.yml` and change the port mapping:
```yaml
ports:
  - "8081:8080"  # Change 8081 to any free port
```

### MongoDB connection issues
```bash
# Access MongoDB shell
docker-compose exec mongodb mongosh -u admin -p admin123

# Check if database exists
show dbs
```

### Redis connection issues
```bash
# Test Redis
docker-compose exec redis redis-cli ping
```

### Backend build fails
```bash
# Rebuild with no cache
docker-compose build --no-cache backend
```

## 💾 Data Persistence

All data is persisted in Docker volumes:
- `mongodb_data` - MongoDB database
- `redis_data` - Redis cache
- `rabbitmq_data` - RabbitMQ messages
- `backend_uploads` - Uploaded files
- `backend_logs` - Application logs
- `grafana_data` - Grafana dashboards
- `prometheus_data` - Metrics

### Backup Data

```bash
# Backup MongoDB
docker-compose exec mongodb mongodump --out /tmp/backup
docker cp mongodb:/tmp/backup ./backup-$(date +%Y%m%d)

# Backup volumes
docker run --rm \
  -v satellite_platform_application_mongodb_data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/mongodb-backup.tar.gz /data
```

## 🔒 Security Notes

**⚠️ Important for Production:**
1. Change all default passwords in `.env` files
2. Use strong JWT secrets (32+ characters)
3. Enable firewall rules
4. Use HTTPS with reverse proxy
5. Regular backups
6. Update service account permissions
7. Monitor logs for suspicious activity

## 📈 Monitoring

Access Grafana at http://localhost:3001 (admin/admin123) to view:
- Application metrics
- System performance
- Database stats
- Cache hit rates
- API response times

## 🧪 Testing Setup

```bash
# Register a test user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"Test@123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test@123"}'
```

## 🔄 Updates

```bash
# Pull latest images
docker-compose pull

# Rebuild and restart
docker-compose up -d --build
```

## 🆘 Getting Help

1. Check service logs: `docker-compose logs -f [service-name]`
2. Verify all services are healthy: `docker-compose ps`
3. Check the main README.md for detailed setup
4. Ensure all `.env` files are configured correctly

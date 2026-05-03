# 🚀 Quick Reference - Satellite Platform Application

## Start/Stop Commands

```bash
# Start everything
docker-compose up -d

# Start with rebuild
docker-compose up -d --build

# Stop everything
docker-compose down

# Stop and remove all data
docker-compose down -v

# Restart specific service
docker-compose restart backend

# View all logs
docker-compose logs -f

# View specific service log
docker-compose logs -f backend
```

## Service URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | - |
| Backend API | http://localhost:8080 | - |
| Swagger UI | http://localhost:8080/swagger-ui.html | - |
| GEE Service | http://localhost:5000 | - |
| Image Processing | http://localhost:8000 | - |
| RabbitMQ UI | http://localhost:15672 | admin/admin123 |
| Grafana | http://localhost:3001 | admin/admin123 |
| Prometheus | http://localhost:9090 | - |

## Quick Health Checks

```bash
# Check all services
docker-compose ps

# Test backend
curl http://localhost:8080/actuator/health

# Test GEE
curl http://localhost:5000/

# Test image processing
curl http://localhost:8000/

# Test Redis
docker-compose exec redis redis-cli ping

# Test MongoDB
docker-compose exec mongodb mongosh --eval "db.adminCommand('ping')"
```

## Debug Commands

```bash
# Enter backend container
docker-compose exec backend sh

# Enter MongoDB shell
docker-compose exec mongodb mongosh -u admin -p admin123

# Enter Redis CLI
docker-compose exec redis redis-cli

# Check logs
docker-compose logs -f [service-name]

# Check container stats
docker stats

# Inspect service
docker inspect [container-name]
```

## File Locations

### Configuration Files
```
Backend/.env                    - Backend environment variables
gee_app_with_cache_logic/.env  - GEE service configuration
FrontEnd/.env.local            - Frontend configuration
```

### Service Account
```
gee_app_with_cache_logic/satellite-platform-application-f7154aa5ce46.json
```

### Data Volumes
```
mongodb_data       - MongoDB database
redis_data         - Redis cache
rabbitmq_data      - RabbitMQ messages
backend_uploads    - Uploaded files
backend_logs       - Application logs
```

## API Testing

### Register User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"Test@123"}'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"Test@123"}'
```

## Troubleshooting

### Service won't start
```bash
docker-compose logs [service-name]
docker-compose restart [service-name]
```

### Rebuild service
```bash
docker-compose build --no-cache [service-name]
docker-compose up -d [service-name]
```

### Clean restart
```bash
docker-compose down -v
docker-compose up -d --build
```

### Port conflict
Edit `docker-compose.yml` and change the port:
```yaml
ports:
  - "NEW_PORT:8080"
```

## Environment Files Summary

### Backend/.env
- MONGO_URI
- REDIS_URL
- JWT_SECRET ⚠️
- ADMIN_PASSWORD ⚠️
- RABBITMQ_* ⚠️

### gee_app_with_cache_logic/.env
- GEE_SERVICE_ACCOUNT
- GEE_PROJECT_ID
- GEE_KEY_FILE

### FrontEnd/.env.local
- NEXT_PUBLIC_API_BASE_URL
- NEXT_PUBLIC_GEE_API_URL
- NEXT_PUBLIC_IMAGE_API_URL

⚠️ = Change in production

## Backup & Restore

### Backup MongoDB
```bash
docker-compose exec mongodb mongodump --out /tmp/backup
docker cp mongodb:/tmp/backup ./backup-$(date +%Y%m%d)
```

### Backup Volumes
```bash
docker run --rm \
  -v satellite_platform_application_mongodb_data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/mongodb-backup.tar.gz /data
```

## Admin Interface

The Admin Dashboard is **exclusively for users with ROLE_ADMIN**. When admins log in, they are automatically redirected to `/admin` instead of `/dashboard`.

### Admin Pages & Features

| Page | URL | Function |
|------|-----|----------|
| Dashboard | `/admin` | Overview & statistics (if dashboard page exists) |
| User Management | `/admin/users` | Create, edit, delete users; lock/unlock accounts; reset passwords; approve/reject admin signup requests |
| Role Management | `/admin/roles` | Create, view, delete roles; assign authorities |
| System Configuration | `/admin/config` | Manage runtime configuration properties |
| Audit Logs | `/admin/audit` | View audit logs by date range, username, action; real-time log streaming |
| Storage Management | `/admin/storage` | Monitor and manage system storage usage |
| Task Management | `/admin/tasks` | Monitor long-running tasks; cancel tasks |
| Project Management | `/admin/projects` | View and manage all projects across the system |

### Admin API Endpoints

All endpoints require `ROLE_ADMIN` and are prefixed with `/api/admin`:

**User Management:**
- `GET /api/admin/users` - List all users
- `POST /api/admin/users?username=...&email=...&password=...&roles=...` - Create user
- `PUT /api/admin/users/{userId}?username=...&email=...&roles=...` - Update user
- `POST /api/admin/users/{userId}/reset-password?newPassword=...` - Reset password
- `POST /api/admin/users/{userId}/{true|false}` - Lock/unlock account
- `DELETE /api/admin/users/{userId}` - Delete user

**Role Management:**
- `GET /api/admin/roles` - List all roles
- `POST /api/admin/roles` - Create role
- `DELETE /api/admin/roles/{roleName}` - Delete role

**Configuration:**
- `GET /api/admin/config/manageable` - Get manageable config properties
- `PUT /api/admin/config/manageable` - Update config property

**Audit Logs:**
- `GET /api/admin/audit/latest?lines=100&username=...&action=...` - Get latest logs
- `GET /api/admin/audit/by-date/{yyyy-MM-dd}` - Logs for specific date
- `GET /api/admin/audit/by-range?startDate=...&endDate=...` - Logs for date range
- `GET /api/admin/audit/available-dates` - List dates with logs

**Admin Signup Requests:**
- `GET /api/admin/signup-requests/pending` - Get pending admin requests
- `POST /api/admin/signup-requests/{requestId}/approve` - Approve request
- `POST /api/admin/signup-requests/{requestId}/reject` - Reject request

### Testing Admin Features

1. Use REST client in `Backend/http/admin/AdminManagement.http`
2. Login as an admin user from http://localhost:3000/auth/login
3. Navigate to Admin Dashboard from header menu or visit http://localhost:3000/admin

### How Admins are Identified

- Users with `ROLE_ADMIN` in their roles array
- After login, admin users see "Admin" link in header navigation
- Admin layout enforces role-based access control

## Development Mode

```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up
```

## Production Checklist

- [ ] Change all default passwords
- [ ] Use strong JWT secret (32+ chars)
- [ ] Enable HTTPS
- [ ] Configure firewall
- [ ] Set up backups
- [ ] Configure monitoring alerts
- [ ] Review security settings

## Common Errors

| Error | Solution |
|-------|----------|
| Port already in use | Change port in docker-compose.yml |
| Connection refused | Check service is running: `docker-compose ps` |
| Permission denied | Check file permissions, especially GEE key |
| Out of memory | Increase Docker memory limit |
| Build failed | Try: `docker-compose build --no-cache` |

## Support Files

- [README.md](README.md) - Complete documentation
- [DOCKER-SETUP.md](DOCKER-SETUP.md) - Docker detailed guide
- [SETUP-CHECKLIST.md](SETUP-CHECKLIST.md) - Step-by-step setup

---

**Quick Start:** `./setup.sh` or `docker-compose up -d`

# Satellite Platform Application - AI Agent Instructions

## Architecture Overview

This is a **microservices-based collaborative platform** for satellite imagery analysis with 4 independent services:

- **Backend** (Spring Boot 3.3.10, Java 17, Port 8080) - Core API, authentication, project/workflow management
- **Frontend** (Next.js 14, React 18, TypeScript, Port 3000) - Web UI with real-time features
- **GEE Service** (Flask/Python, Port 5000) - Google Earth Engine integration with Redis caching
- **Image Processing** (FastAPI/Python, Port 8000) - Satellite image processing (NDVI, vegetation indices)

**Infrastructure:** MongoDB (primary DB), Redis (caching/rate limiting), RabbitMQ (message broker), Prometheus/Grafana (monitoring)

## Critical Module Organization

Backend uses **domain-driven modules** under `Backend/src/main/java/com/enit/satellite_platform/modules/`:
- `user_management/` - JWT auth (access + refresh tokens), role-based access (ADMIN, THEMATICIAN)
- `project_management/` - Projects with ownership validation, cascading deletes
- `resource_management/` - Image storage, processing results, GeoTools integration
- `workflow/` - Event-driven workflow engine with node execution framework (Trigger, GEE, Image Processing, Output nodes)
- `messaging/` - Real-time WebSocket messaging with presence system (STOMP over SockJS)
- `community/` - User communities and collaboration features
- `dashboard/` - Analytics and statistics
- `activity/` - User activity tracking and logging

## Development Workflows

### Starting Services

```bash
# Full stack (recommended)
docker-compose up -d

# Backend only (requires MongoDB + Redis running)
cd Backend && ./mvnw spring-boot:run

# Frontend dev server
cd FrontEnd && npm run dev

# GEE service
cd gee_app_with_cache_logic && python app.py
```

**Port Configuration:** Backend runs on **8080** (default Spring Boot). Always use `http://localhost:8080` for API calls.

### Testing Approach

- **Backend:** Use `.http` files in `Backend/http/*/` directories with IntelliJ/VS Code REST Client
- **Example:** `Backend/http/user_management/Authentication.http` for auth testing, `Backend/http/community/Publications.http` for publications
- **No workflow trigger .http files exist** - workflows are tested via frontend UI at `/workflows`
- **Frontend:** No formal test suite; manual testing via UI

### Environment Configuration

**Critical:** Backend uses `spring-dotenv` to load environment variables from `Backend/.env` (never commit):
1. Required vars: `MONGO_URI`, `REDIS_URL`, `JWT_SECRET`, `JWT_EXPIRATION`, `PYTHON_BASE_URL`, `GEE_BASE_URL`, `APP1_URL`
2. Frontend: Create `FrontEnd/.env.local` with `NEXT_PUBLIC_API_URL=http://localhost:8080`
3. Storage paths: `PROJECT_BASE_PATH`, `IMAGE_STORAGE_PATH`, `TEMP_STORAGE_PATH` must be absolute paths
4. RabbitMQ: `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASS`

## Key Patterns & Conventions

### 1. Authentication Flow

**Backend:** JWT with dual-token system (access + refresh). Tokens stored in HTTP-only cookies on frontend.
- Access token: Short-lived (configurable via `JWT_EXPIRATION`)
- Refresh token: Used to obtain new access tokens at `/api/auth/refresh`
- Frontend middleware (`FrontEnd/src/middleware.ts`) reads `accessToken` cookie, adds `x-user-authenticated` header

### 2. Real-Time Features (WebSocket)

**Connect:** `ws://localhost:8080/ws` with JWT auth via `WebSocketAuthInterceptor`
- **Messaging:** `/app/chat.send` (send) → `/topic/messages/{conversationId}` (receive)
- **Presence:** Auto-subscribes to `/topic/presence` for online/offline status
- **Typing indicators:** `/app/chat.typing` → `/topic/typing/{conversationId}`
- **Frontend hook:** `FrontEnd/src/hooks/useMessaging.ts` manages state

**Critical:** Backend tracks users by ObjectId (not email). `UserPresenceService` converts email → ObjectId on connect.

### 3. Workflow System Architecture

Workflows are visual node-based programs with 6 node types in `Backend/modules/workflow/execution/nodes/`:
- `TriggerNodeExecutor` - Entry point (manual/scheduled/webhook/event triggers)
- `GEENodeExecutor` - Google Earth Engine satellite data fetching
- `ImageProcessingNodeExecutor` - NDVI, EVI, vegetation indices calculations
- `OutputNodeExecutor` - Result persistence and export
- `DecisionNodeExecutor` - Conditional branching logic
- `DataTransformNodeExecutor` - Data transformation operations

**Workflow Versioning:** Each edit creates new version (v1.0 → v1.1). Current version tracked in `Workflow.currentVersion`.

**Testing:** Frontend at `http://localhost:3000/workflows` - no `.http` files or test scripts exist for workflows.

### 4. MongoDB Patterns

- **Compound indexes** enforce data integrity (e.g., unique project names per user)
- **Cascading deletes** via Spring Data MongoDB lifecycle events
- **Audit fields:** All entities have `createdAt`, `updatedAt` timestamps
- Repository naming: `*Repository.java` for basic CRUD, `*RepositoryImpl.java` for custom queries

### 5. Python Service Integration

Backend calls Python services via `RestClient` (WebFlux):
- **GEE Service:** `${PYTHON_BASE_URL}/api/gee/*` (e.g., `/fetch-images`)
- **Image Processing:** `${PYTHON_BASE_URL}/api/process/*`
- **Caching:** GEE service uses Redis to cache satellite queries (check `gee_app_with_cache_logic/gee_app/services/`)

## Frontend Architecture

- **App Router** (Next.js 14): Routes in `FrontEnd/src/app/*/page.tsx`
- **State Management:** React hooks + custom hooks (no Redux/Zustand)
- **Styling:** Tailwind CSS + Material-UI v7 components
- **API Calls:** Axios with token from cookies (`FrontEnd/src/services/*Api.ts`)
- **Map Integration:** Leaflet with `react-leaflet` for geospatial visualization
- **Workflow Builder:** `@xyflow/react` for visual node graph editor

**Critical:** Always handle WebSocket reconnection in hooks. Connection status tracked in `wsService.isConnected`.

## Cross-Service Communication

- **Backend → GEE/Image Processing:** HTTP REST (synchronous)
- **Backend → Frontend:** REST API + WebSocket (for real-time updates)
- **Inter-module (Backend):** Direct service injection (no microservice mesh within Backend)
- **Monitoring:** All services expose Prometheus metrics at `/actuator/prometheus` (Backend) or `/metrics` (Python)

## Documentation Files

- `QUICK-REFERENCE.md` - Service URLs, health checks, debug commands
- `Backend/PHASE_7_EVENT_DRIVEN_TRIGGERS_COMPLETE.md` - Workflow trigger system details
- `Backend/PRESENCE_SYSTEM.md` - WebSocket presence tracking implementation
- `FrontEnd/MESSAGING_INTEGRATION_COMPLETE.md` - Real-time messaging guide
- `DOCKER-SETUP.md` - Container orchestration and ports

## Common Pitfalls

1. **Port confusion:** Backend is 8080, not 9090
2. **Auth tokens:** Use ObjectId for backend operations, not email strings
3. **WebSocket auth:** Must include JWT in handshake; plain Stomp.js doesn't support this - use `WebSocketAuthInterceptor`
4. **GEE credentials:** Service account JSON must be mounted in Docker (never commit)
5. **Cron expressions:** Backend scheduler uses 6-field cron (includes seconds): `0 */5 * * * *` = every 5 minutes
6. **MongoDB ObjectId:** Always validate ID format before querying; invalid IDs cause 500 errors

## When Adding Features

- **New backend endpoint:** Add to appropriate module's `controllers/`, create DTO in module's package, update corresponding `.http` file
- **New entity:** Add to module's `entities/`, create repository, add indexes via `@CompoundIndex` annotation
- **New frontend page:** Create in `src/app/*/page.tsx`, add route to navigation, handle auth state
- **New Python service:** Add route in `routes/`, implement service in `services/`, update `docker-compose.yml` port mappings

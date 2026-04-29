# Architecture Knowledge Base

Last updated: 2026-04-29

This document summarizes the architecture from code and configuration, focusing on how the platform is composed and how data moves.

## 1) Platform Topology

Primary runtime pieces:

1. Frontend (Next.js / React / TypeScript)
2. Backend (Spring Boot 3, Java 17)
3. GEE service (Flask/Python)
4. Image-processing service (FastAPI/Python)

Supporting infrastructure:

- MongoDB (primary persistence)
- Redis (cache / rate-limiting support)
- RabbitMQ (async workflow execution queue)
- STOMP/WebSocket channel for realtime events

Reference setup docs:
- [README.md](../README.md)
- [DOCKER-SETUP.md](../DOCKER-SETUP.md)

## 2) Backend Module Structure

Backend module root:
- [Backend/src/main/java/com/enit/satellite_platform/modules](../Backend/src/main/java/com/enit/satellite_platform/modules)

Main modules:

- `user_management`
  - Auth, registration, role administration, device/session management
- `project_management`
  - Project lifecycle, sharing, archive/restore, tag/status filtering
- `resource_management`
  - Image persistence, processing result management, geospatial integrations
- `workflow`
  - Visual workflow persistence, versioning, orchestration, queue-backed execution
- `messaging`
  - Conversations/messages, presence, WebSocket signaling
- `community`
  - Publications and user discovery
- `dashboard`
  - Aggregated user/project/activity stats
- `activity`
  - Activity tracking support (internal)

## 3) Frontend Structure

Frontend source root:
- [FrontEnd/src](../FrontEnd/src)

Key directories:

- `app/` route handlers (Next.js app router)
- `services/` API clients for backend calls
- `hooks/` page-level and domain-level state/effect logic
- `components/` reusable UI building blocks
- `types/` shared TypeScript contracts
- `utils/` HTTP client utilities and helpers

## 4) Key Cross-Cutting Patterns

### Authentication

- Backend auth controller under `/api` provides signin/signup/refresh/reset paths.
- Frontend uses cookie-aware calls (and legacy token paths in some features).
- WebSocket auth is integrated with STOMP connection interception.

Relevant code:
- [Backend/src/main/java/com/enit/satellite_platform/modules/user_management/normal_user_service/controllers/AuthController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/user_management/normal_user_service/controllers/AuthController.java)
- [Backend/src/main/java/com/enit/satellite_platform/modules/messaging/websocket/WebSocketAuthInterceptor.java](../Backend/src/main/java/com/enit/satellite_platform/modules/messaging/websocket/WebSocketAuthInterceptor.java)
- [FrontEnd/src/services/websocketService.ts](../FrontEnd/src/services/websocketService.ts)

### Shared API Response Envelope

Many backend controllers return a `GenericResponse` wrapper (`status`, `message`, `data`), while some endpoints may return domain types directly.

Implication:
- Frontend service methods should normalize data shape consistently to avoid per-page parsing divergence.

### Async + Realtime Workflow Pattern

Workflow execution path:

1. Client requests execution.
2. Backend creates execution record.
3. Execution id sent to RabbitMQ queue.
4. Listener executes node graph.
5. Execution updates broadcast over `/topic/workflow.execution.{executionId}`.

Relevant code:
- [Backend/src/main/java/com/enit/satellite_platform/modules/workflow/services/WorkflowExecutionService.java](../Backend/src/main/java/com/enit/satellite_platform/modules/workflow/services/WorkflowExecutionService.java)
- [Backend/src/main/java/com/enit/satellite_platform/modules/workflow/config/WorkflowRabbitMQConfig.java](../Backend/src/main/java/com/enit/satellite_platform/modules/workflow/config/WorkflowRabbitMQConfig.java)
- [FrontEnd/src/app/workflows/[id]/page.tsx](../FrontEnd/src/app/workflows/[id]/page.tsx)

## 5) Data Domain Summary

Core persisted entities (high-level):

- Users / roles / signup requests / refresh tokens
- Projects + sharing metadata
- Images + processing results
- Workflows + versions + executions + logs
- Messaging conversations + messages + presence state
- Community publications + likes/tags/statuses
- Audit/activity events

## 6) Integration Boundaries

### Backend ↔ Python Services

- GEE proxy and retrieval through backend geospatial modules
- Vegetation-index and processing operations through backend integration layer
- Workflow node executors call those services indirectly via backend service abstractions

### Backend ↔ Frontend

- REST calls via frontend service clients
- STOMP topics/user queues for realtime interaction

### Backend ↔ Infrastructure

- Mongo repositories and query-heavy controllers
- Redis for caching/rate-limiting concerns
- Rabbit queue for long-running workflow orchestration

## 7) Known Architectural Watchpoints

1. Mixed auth styles (cookie-first and token-first traces) across older/newer features.
2. Large controller surfaces in some modules (especially projects/images) make endpoint drift easy.
3. Some services have both CRUD and advanced operational concerns in the same class.
4. Queue + websocket reliability and observability should be treated as production hardening tasks.

## 8) Recommended Reading Order for New Contributors

1. [README.md](../README.md)
2. [docs/BACKEND_API_SURVEY.md](BACKEND_API_SURVEY.md)
3. [docs/FRONTEND_ROUTE_SERVICE_MAP.md](FRONTEND_ROUTE_SERVICE_MAP.md)
4. [WORKFLOW_IMPLEMENTATION.md](../WORKFLOW_IMPLEMENTATION.md)
5. [Backend/PRESENCE_SYSTEM.md](../Backend/PRESENCE_SYSTEM.md)

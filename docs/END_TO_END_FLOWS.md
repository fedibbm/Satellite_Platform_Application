# End-to-End Flow Guide

Last updated: 2026-04-29

This guide documents high-value user flows across frontend, backend, and realtime/async layers.

## 1) Authentication Flow

Primary files:
- Frontend auth pages: [FrontEnd/src/app/auth](../FrontEnd/src/app/auth)
- Frontend auth hook: [FrontEnd/src/hooks/useAuth.tsx](../FrontEnd/src/hooks/useAuth.tsx)
- Backend auth controller: [Backend/src/main/java/com/enit/satellite_platform/modules/user_management/normal_user_service/controllers/AuthController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/user_management/normal_user_service/controllers/AuthController.java)

Flow summary:
1. User signs in/signs up via frontend auth route.
2. Frontend service calls backend `/api/auth/...` endpoints.
3. Backend issues auth/session artifacts and returns account metadata.
4. Frontend updates auth context and route guards.

## 2) Project Lifecycle Flow

Primary files:
- Project pages: [FrontEnd/src/app/projects](../FrontEnd/src/app/projects)
- Project services: [FrontEnd/src/services/projects.service.ts](../FrontEnd/src/services/projects.service.ts)
- Backend project controller: [Backend/src/main/java/com/enit/satellite_platform/modules/project_management/controllers/ProjectController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/project_management/controllers/ProjectController.java)

Flow summary:
1. User creates/updates project from frontend pages.
2. Service layer calls `/api/thematician/projects/...`.
3. Backend persists lifecycle state (active, archived, deleted) and sharing metadata.
4. Frontend merges owned/shared lists and renders filters.

## 3) Workflow Execution Flow

Primary files:
- Workflow pages: [FrontEnd/src/app/workflows](../FrontEnd/src/app/workflows)
- Workflow service: [FrontEnd/src/services/workflow.service.ts](../FrontEnd/src/services/workflow.service.ts)
- WebSocket client: [FrontEnd/src/services/websocketService.ts](../FrontEnd/src/services/websocketService.ts)
- Workflow controller: [Backend/src/main/java/com/enit/satellite_platform/modules/workflow/controllers/WorkflowController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/workflow/controllers/WorkflowController.java)
- Workflow execution service: [Backend/src/main/java/com/enit/satellite_platform/modules/workflow/services/WorkflowExecutionService.java](../Backend/src/main/java/com/enit/satellite_platform/modules/workflow/services/WorkflowExecutionService.java)

Flow summary:
1. User triggers execution from workflow UI.
2. Backend validates graph and creates execution record.
3. Execution job is queued (RabbitMQ path).
4. Listener executes staged nodes via executors.
5. Progress/state events are broadcast on workflow topic.
6. Frontend receives realtime events and updates execution view.

## 4) Messaging Flow (REST + STOMP)

Primary files:
- Messaging page: [FrontEnd/src/app/messages/page.tsx](../FrontEnd/src/app/messages/page.tsx)
- Messaging hook: [FrontEnd/src/hooks/useMessaging.ts](../FrontEnd/src/hooks/useMessaging.ts)
- Messaging REST service: [FrontEnd/src/services/messagingApi.ts](../FrontEnd/src/services/messagingApi.ts)
- Messaging websocket service: [FrontEnd/src/services/websocketService.ts](../FrontEnd/src/services/websocketService.ts)
- Messaging controller: [Backend/src/main/java/com/enit/satellite_platform/modules/messaging/controllers/MessagingController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/messaging/controllers/MessagingController.java)
- WebSocket messaging controller: [Backend/src/main/java/com/enit/satellite_platform/modules/messaging/websocket/WebSocketMessagingController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/messaging/websocket/WebSocketMessagingController.java)

Flow summary:
1. Frontend bootstraps conversations/messages over REST.
2. Client subscribes to user-specific queues over STOMP.
3. Send/read/typing/status events travel through websocket mappings.
4. Backend pushes targeted updates to destination queues.

## 5) Image/Analysis Flow

Primary files:
- Analysis page: [FrontEnd/src/app/analysis/page.tsx](../FrontEnd/src/app/analysis/page.tsx)
- Image service: [FrontEnd/src/services/images.service.ts](../FrontEnd/src/services/images.service.ts)
- GEE service adapter: [FrontEnd/src/services/gee.service.ts](../FrontEnd/src/services/gee.service.ts)
- Backend image controller: [Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/image_management/controllers/ImageController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/image_management/controllers/ImageController.java)
- Backend processing controller: [Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/image_management/controllers/ProcessingResultsController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/image_management/controllers/ProcessingResultsController.java)

Flow summary:
1. User uploads/selects images in project context.
2. Backend stores metadata/files and associates with project.
3. Processing/analysis requests run through backend geospatial adapters.
4. Results are stored, listed, and optionally downloaded.

## 6) Operational Risks to Track

1. Dual communication mode (REST + realtime) requires consistent state reconciliation on reconnect.
2. Long-running workflows/geospatial tasks require explicit timeout/retry policies.
3. Response envelopes are not fully uniform across modules; normalize in frontend services.
4. Shared-vs-owned resource list merges must remain deduplicated and permission-safe.

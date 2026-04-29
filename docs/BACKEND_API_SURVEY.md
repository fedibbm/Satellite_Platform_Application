# Backend API Survey

Last updated: 2026-04-29

This survey maps controllers and endpoint families by module. It is intended as a practical orientation guide, not a strict OpenAPI replacement.

Backend source root:
- [Backend/src/main/java/com/enit/satellite_platform/modules](../Backend/src/main/java/com/enit/satellite_platform/modules)

## 1) Authentication and User Management

### AuthController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/user_management/normal_user_service/controllers/AuthController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/user_management/normal_user_service/controllers/AuthController.java)

Base path: `/api`

Main routes:
- `POST /auth/signin`
- `POST /auth/signup`
- `GET /auth/roles`
- `POST /auth/refresh`
- `POST /auth/logout`
- `POST /forgot-password`
- `POST /reset-password`
- `PUT /account/update/{id}`
- `GET /thematician/account/{id}`
- `DELETE /thematician/signout/{id}`
- `GET /users`

### DeviceManagementController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/user_management/management_cvore_service/security/device/controller/DeviceManagementController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/user_management/management_cvore_service/security/device/controller/DeviceManagementController.java)

Base path: `/api/devices`

Main routes:
- `GET /active`
- `POST /{deviceId}/approve`
- `POST /{deviceId}/revoke`
- `POST /cleanup`

## 2) Admin APIs

### AdminController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/user_management/admin_privileges/controller/AdminController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/user_management/admin_privileges/controller/AdminController.java)

Base path: `/api/admin`

Main routes:
- `GET /config/manageable`
- `PUT /config/manageable`
- `POST /users`
- `GET /users`
- `PUT /users/{userId}`
- `DELETE /users/{userId}`
- `POST /users/{userId}/reset-password`
- `POST /users/{userId}/{lock}`
- Signup request moderation routes (`/signup-requests/...`)

### AdminConfigController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/user_management/admin_privileges/controller/AdminConfigController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/user_management/admin_privileges/controller/AdminConfigController.java)

Base path: `/api/admin/config`

Main routes include:
- `GET /{prefix}`
- `POST /batch`
- `PUT /config/runtime`

### RoleController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/user_management/admin_privileges/controller/RoleController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/user_management/admin_privileges/controller/RoleController.java)

Base path: `/api/admin/roles`

Main routes:
- `POST /`
- `GET /`
- `GET /{roleName}`
- `DELETE /{roleName}`

### AuditController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/user_management/admin_privileges/controller/AuditController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/user_management/admin_privileges/controller/AuditController.java)

Base path: `/api/admin/audit`

Main routes:
- `GET /latest`
- `GET /by-date/{dateString}`
- `GET /by-range`
- `GET /available-dates`

## 3) Project Management APIs

### ProjectController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/project_management/controllers/ProjectController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/project_management/controllers/ProjectController.java)

Base path: `/api/thematician/projects`

Main route families:

- Core CRUD:
  - `POST /create`
  - `GET /{projectId}`
  - `PUT /{projectId}`
  - `DELETE /{projectId}`
  - `PUT /{id}/rename`

- Listing and filtering:
  - `GET /all`
  - `GET /search`
  - `GET /by-status`
  - `GET /by-tag`
  - `GET /archived`
  - `GET /deleted`

- Sharing:
  - `POST /share`, `POST /unshare`
  - `POST /{projectId}/share`, `POST /{projectId}/unshare`
  - `GET /{projectId}/shared-users`
  - `GET /shared-with-me`

- Lifecycle and retention:
  - `POST /{projectId}/archive`
  - `POST /{projectId}/unarchive`
  - `POST /{projectId}/restore`
  - `DELETE /{projectId}/force`
  - `PUT /{projectId}/retention`

- Utility:
  - `GET /statistics`
  - `GET /last-accessed`
  - `GET /{projectId}/export`
  - `POST /{projectId}/duplicate`
  - `POST /createFromTemplate`
  - `DELETE /bulk-delete`

## 4) Resource Management APIs

### ImageController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/image_management/controllers/ImageController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/image_management/controllers/ImageController.java)

Base path: `/geospatial/images`

Main routes:
- `POST /add`
- `GET /{id}`
- `GET /{id}/data`
- `PUT /{id}/rename`
- `DELETE /{id}`
- `GET /by-project/{projectId}`
- `DELETE /by-project/{projectId}`
- `GET /count/{projectId}`
- `POST /import`
- soft-delete lifecycle routes (`/{id}/restore`, `/{id}/force`, `/deleted`)
- cross-reference routes (`/{imageId}/project/{projectId}`)
- bulk routes (`/bulk-delete`)

### ProcessingResultsController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/image_management/controllers/ProcessingResultsController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/image_management/controllers/ProcessingResultsController.java)

Base path: `/geospatial/processing`

Main routes:
- `POST /save`
- `POST /bulk-save`
- `GET /{id}`
- `GET /{id}/file`
- `GET /image/{imageId}`
- `GET /project/{projectId}`
- `PUT /{id}`
- `DELETE /{id}`
- soft-delete lifecycle routes (`/{id}/restore`, `/{id}/force`, `/deleted`)

### VegetationIndexController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/GeoSpacialTools/openCV/vegetation_Index_calculation/VegetationIndexController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/GeoSpacialTools/openCV/vegetation_Index_calculation/VegetationIndexController.java)

Base path: `/api/v1/vegetation-indices`

Main routes:
- `POST /ndvi`
- `POST /evi`

### GeeController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/GeoSpacialTools/gee/controller/GeeController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/GeoSpacialTools/gee/controller/GeeController.java)

Base path: `/geospatial/gee`

Main route:
- `POST /service`

### GeoSpatialController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/GeoSpacialTools/geotools/controller/GeoSpatialController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/GeoSpacialTools/geotools/controller/GeoSpatialController.java)

Base path: `/geospatial/geotools`

Main routes include:
- `/upload`, `/ndvi`, `/evi`, `/water-detection`, `/building-detection`, `/land-cover`
- calibration routes (`/calibrate`, `/calibration/{sensorName}`)

### TaskController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/image_management/controllers/TaskController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/resource_management/image_management/controllers/TaskController.java)

Base path: `/tasks`

Note: task operations are exposed here for async/background processing visibility.

## 5) Workflow APIs

### WorkflowController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/workflow/controllers/WorkflowController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/workflow/controllers/WorkflowController.java)

Base path: `/api/workflows`

Main routes:
- `GET /`
- `GET /templates`
- `GET /project/{projectId}`
- `GET /{id}`
- `POST /`
- `POST /{id}/copy`
- `PUT /{id}`
- `DELETE /{id}`
- `POST /{id}/execute`
- `GET /{id}/executions`
- `GET /executions/{executionId}`

## 6) Messaging and Realtime APIs

### MessagingController (REST)
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/messaging/controllers/MessagingController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/messaging/controllers/MessagingController.java)

Base path: `/api/messaging`

Main routes:
- Message send/read:
  - `POST /messages`
  - `POST /messages/image`
  - `PUT /messages/{id}/read`
- Conversation:
  - `GET /conversations`
  - `GET /conversations/{id}`
  - `GET /conversations/{id}/messages`
  - `PUT /conversations/{id}/read`
  - `DELETE /conversations/{id}`
- Presence/utility:
  - `GET /unread-count`
  - `GET /online-users`
  - `GET /users/{userId}/online`
  - `GET /images/{conversationId}/{filename}`

### WebSocketMessagingController (STOMP)
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/messaging/websocket/WebSocketMessagingController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/messaging/websocket/WebSocketMessagingController.java)

Main message mappings:
- `/app/chat.send`
- `/app/chat.typing`
- `/app/chat.read`
- `/app/chat.status`

Main user destinations:
- `/user/queue/messages`
- `/user/queue/typing`
- `/user/queue/receipts`
- `/user/queue/status`
- `/user/queue/errors`

## 7) Community APIs

### PublicationController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/community/controllers/PublicationController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/community/controllers/PublicationController.java)

Base path: `/api/community/publications`

Main routes:
- `POST /`
- `GET /`
- `GET /{id}`
- `PUT /{id}`
- `DELETE /{id}`
- `POST /{id}/like`
- `GET /search`
- `GET /tag/{tag}`
- `GET /trending`
- `GET /author/{email}`
- `GET /my`

### CommunityUserController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/community/controllers/CommunityUserController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/community/controllers/CommunityUserController.java)

Base path: `/api/community`

Main route:
- `GET /users`

## 8) Dashboard/Storage APIs

### DashboardController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/dashboard/controller/DashboardController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/dashboard/controller/DashboardController.java)

Base path: `/api/dashboard`

Main route:
- `GET /stats`

### StorageController
File:
- [Backend/src/main/java/com/enit/satellite_platform/modules/user_management/normal_user_service/controllers/StorageController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/user_management/normal_user_service/controllers/StorageController.java)

Base path: `/admin/storage`

Main route:
- `GET /usage`

## 9) Notes for API Consumers

1. Response envelope consistency varies by controller (some direct entities, many `GenericResponse`).
2. For workflow and messaging, prefer consuming by dedicated frontend service adapters.
3. For realtime, pair REST bootstrap endpoints with WebSocket subscriptions.
4. For project sharing, both generic (`/share`) and project-scoped (`/{projectId}/share`) endpoints exist; prefer one convention per client.

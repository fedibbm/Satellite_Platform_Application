# Satellite Platform Documentation Index

Last updated: 2026-04-29

This index is a navigation map for the project documentation and code entry points.

## 1) Start Here

- Main platform overview: [README.md](../README.md)
- Environment and deployment setup: [DOCKER-SETUP.md](../DOCKER-SETUP.md)
- Setup checklist: [SETUP-CHECKLIST.md](../SETUP-CHECKLIST.md)
- Quick command/runtime reference: [QUICK-REFERENCE.md](../QUICK-REFERENCE.md)

## 2) Workflow Engine Docs

- Workflow implementation state: [WORKFLOW_IMPLEMENTATION.md](../WORKFLOW_IMPLEMENTATION.md)
- Workflow quick start/testing: [WORKFLOW_QUICK_START.md](../WORKFLOW_QUICK_START.md)
- Phase tracking and backlog: [WORKFLOW_PHASE_2_TRACKER.md](../WORKFLOW_PHASE_2_TRACKER.md)
- Architecture/pattern notes: [WORKFLOW_ARCHITECTURE_AND_PATTERNS.md](../WORKFLOW_ARCHITECTURE_AND_PATTERNS.md)

## 3) Messaging and Realtime Docs

- Presence system: [Backend/PRESENCE_SYSTEM.md](../Backend/PRESENCE_SYSTEM.md)
- WebSocket guide: [Backend/src/main/java/com/enit/satellite_platform/modules/messaging/WEBSOCKET_GUIDE.md](../Backend/src/main/java/com/enit/satellite_platform/modules/messaging/WEBSOCKET_GUIDE.md)
- WebSocket implementation summary: [Backend/src/main/java/com/enit/satellite_platform/modules/messaging/WEBSOCKET_COMPLETE.md](../Backend/src/main/java/com/enit/satellite_platform/modules/messaging/WEBSOCKET_COMPLETE.md)

## 4) Data and Performance Docs

- Data optimization assessment: [DATA_OPTIMIZATION_ASSESSMENT.md](../DATA_OPTIMIZATION_ASSESSMENT.md)
- Buffered writes report: [DATA_OPTIMIZATION_BUFFERED_WRITES_REPORT.md](../DATA_OPTIMIZATION_BUFFERED_WRITES_REPORT.md)

## 5) New High-Coverage Project Docs (This pass)

- System architecture map: [ARCHITECTURE_KNOWLEDGE_BASE.md](ARCHITECTURE_KNOWLEDGE_BASE.md)
- Backend API survey: [BACKEND_API_SURVEY.md](BACKEND_API_SURVEY.md)
- Frontend route/service map: [FRONTEND_ROUTE_SERVICE_MAP.md](FRONTEND_ROUTE_SERVICE_MAP.md)

## 6) Key Code Entry Points

### Backend

- Module root: [Backend/src/main/java/com/enit/satellite_platform/modules](../Backend/src/main/java/com/enit/satellite_platform/modules)
- Workflow controller: [Backend/src/main/java/com/enit/satellite_platform/modules/workflow/controllers/WorkflowController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/workflow/controllers/WorkflowController.java)
- Project controller: [Backend/src/main/java/com/enit/satellite_platform/modules/project_management/controllers/ProjectController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/project_management/controllers/ProjectController.java)
- Messaging REST controller: [Backend/src/main/java/com/enit/satellite_platform/modules/messaging/controllers/MessagingController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/messaging/controllers/MessagingController.java)
- Auth controller: [Backend/src/main/java/com/enit/satellite_platform/modules/user_management/normal_user_service/controllers/AuthController.java](../Backend/src/main/java/com/enit/satellite_platform/modules/user_management/normal_user_service/controllers/AuthController.java)

### Frontend

- App routes root: [FrontEnd/src/app](../FrontEnd/src/app)
- Services: [FrontEnd/src/services](../FrontEnd/src/services)
- Hooks: [FrontEnd/src/hooks](../FrontEnd/src/hooks)
- Workflow pages: [FrontEnd/src/app/workflows](../FrontEnd/src/app/workflows)
- Projects pages: [FrontEnd/src/app/projects](../FrontEnd/src/app/projects)

## 7) Microservice/External Service Repos in Workspace

- GEE Flask service: [gee_app_with_cache_logic/README.md](../gee_app_with_cache_logic/README.md)
- Image processing service: [image_porcessing/app](../image_porcessing/app)
- RSIC FastAPI service: [rsic/README_API.md](../rsic/README_API.md)
- Urban change API: [urban-change-api/README_API.md](../urban-change-api/README_API.md)

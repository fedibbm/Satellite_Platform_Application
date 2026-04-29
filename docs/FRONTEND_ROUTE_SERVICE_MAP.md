# Frontend Route and Service Map

Last updated: 2026-04-29

This document maps frontend routes, major page responsibilities, and the primary service/hook dependencies used across the app.

Frontend roots:
- App routes: [FrontEnd/src/app](../FrontEnd/src/app)
- Services: [FrontEnd/src/services](../FrontEnd/src/services)
- Hooks: [FrontEnd/src/hooks](../FrontEnd/src/hooks)
- Components: [FrontEnd/src/components](../FrontEnd/src/components)

## 1) Route Groups

### Public / Entry

- `/` -> [FrontEnd/src/app/page.tsx](../FrontEnd/src/app/page.tsx)
- `/auth/login` -> [FrontEnd/src/app/auth/login/page.tsx](../FrontEnd/src/app/auth/login/page.tsx)
- `/auth/register` -> [FrontEnd/src/app/auth/register/page.tsx](../FrontEnd/src/app/auth/register/page.tsx)
- `/auth/reset-password` -> [FrontEnd/src/app/auth/reset-password/page.tsx](../FrontEnd/src/app/auth/reset-password/page.tsx)

### User Dashboard and Profile

- `/dashboard` -> [FrontEnd/src/app/dashboard/page.tsx](../FrontEnd/src/app/dashboard/page.tsx)
- `/profile` -> [FrontEnd/src/app/profile/page.tsx](../FrontEnd/src/app/profile/page.tsx)

### Projects Domain

- `/projects` -> [FrontEnd/src/app/projects/page.tsx](../FrontEnd/src/app/projects/page.tsx)
- `/projects/new` -> [FrontEnd/src/app/projects/new/page.tsx](../FrontEnd/src/app/projects/new/page.tsx)
- `/projects/[id]` -> [FrontEnd/src/app/projects/[id]/page.tsx](../FrontEnd/src/app/projects/[id]/page.tsx)
- `/projects/update/[id]` -> [FrontEnd/src/app/projects/update/[id]/page.tsx](../FrontEnd/src/app/projects/update/[id]/page.tsx)

### Workflow Domain

- `/workflows` -> [FrontEnd/src/app/workflows/page.tsx](../FrontEnd/src/app/workflows/page.tsx)
- `/workflows/new` -> [FrontEnd/src/app/workflows/new/page.tsx](../FrontEnd/src/app/workflows/new/page.tsx)
- `/workflows/[id]` -> [FrontEnd/src/app/workflows/[id]/page.tsx](../FrontEnd/src/app/workflows/[id]/page.tsx)

### Geospatial / Analysis

- `/map` -> [FrontEnd/src/app/map/page.tsx](../FrontEnd/src/app/map/page.tsx)
- `/analysis` -> [FrontEnd/src/app/analysis/page.tsx](../FrontEnd/src/app/analysis/page.tsx)
- `/storage` -> [FrontEnd/src/app/storage/page.tsx](../FrontEnd/src/app/storage/page.tsx)

### Messaging and Community

- `/messages` -> [FrontEnd/src/app/messages/page.tsx](../FrontEnd/src/app/messages/page.tsx)
- `/community/users` -> [FrontEnd/src/app/community/users/page.tsx](../FrontEnd/src/app/community/users/page.tsx)
- `/community/forums` -> [FrontEnd/src/app/community/forums/page.tsx](../FrontEnd/src/app/community/forums/page.tsx)
- `/community/publications` -> [FrontEnd/src/app/community/publications/page.tsx](../FrontEnd/src/app/community/publications/page.tsx)
- `/community/publications/create` -> [FrontEnd/src/app/community/publications/create/page.tsx](../FrontEnd/src/app/community/publications/create/page.tsx)
- `/community/publications/[id]` -> [FrontEnd/src/app/community/publications/[id]/page.tsx](../FrontEnd/src/app/community/publications/[id]/page.tsx)

### Admin Area

- `/admin/dashboard`
- `/admin/users`
- `/admin/projects`
- `/admin/roles`
- `/admin/config`
- `/admin/audit`
- `/admin/storage`
- `/admin/tasks`

Files are under [FrontEnd/src/app/admin](../FrontEnd/src/app/admin).

## 2) Service Layer Map

Service directory:
- [FrontEnd/src/services](../FrontEnd/src/services)

Main API clients:

- [auth.service.ts](../FrontEnd/src/services/auth.service.ts)
  - login/register/logout/roles and user account interactions.
- [projects.service.ts](../FrontEnd/src/services/projects.service.ts)
  - project CRUD, sharing, archive, restore, pagination.
- [images.service.ts](../FrontEnd/src/services/images.service.ts)
  - image upload/read/remove and image domain calls.
- [workflow.service.ts](../FrontEnd/src/services/workflow.service.ts)
  - workflow CRUD, templates, execute, execution history.
- [analysis.service.ts](../FrontEnd/src/services/analysis.service.ts)
  - analysis/result operations.
- [gee.service.ts](../FrontEnd/src/services/gee.service.ts)
  - geospatial/GEE requests.
- [messagingApi.ts](../FrontEnd/src/services/messagingApi.ts)
  - REST messaging operations.
- [websocketService.ts](../FrontEnd/src/services/websocketService.ts)
  - STOMP/SockJS realtime signaling.
- [publicationService.ts](../FrontEnd/src/services/publicationService.ts)
  - community publication operations.
- [community.service.ts](../FrontEnd/src/services/community.service.ts)
  - community user listing.
- [dashboard.service.ts](../FrontEnd/src/services/dashboard.service.ts)
  - dashboard metrics retrieval.
- [admin.service.ts](../FrontEnd/src/services/admin.service.ts)
  - admin user/config/audit operations.

## 3) Hooks and Feature Composition

Hooks directory:
- [FrontEnd/src/hooks](../FrontEnd/src/hooks)

Notable hooks:

- [useAuth.tsx](../FrontEnd/src/hooks/useAuth.tsx)
  - auth context/session state.
- [useProjectData.ts](../FrontEnd/src/hooks/useProjectData.ts)
  - project detail load, project actions (archive/delete/unarchive).
- [useProjectImages.ts](../FrontEnd/src/hooks/useProjectImages.ts)
  - image listing, upload, selection, annotations.
- [useProjectAnalysis.ts](../FrontEnd/src/hooks/useProjectAnalysis.ts)
  - processing result listing/edit/delete.
- [useProjectSharing.ts](../FrontEnd/src/hooks/useProjectSharing.ts)
  - collaborator load/share/unshare/permission updates.
- [useGeeImage.ts](../FrontEnd/src/hooks/useGeeImage.ts)
  - gee image fetch/display state.
- [useMessaging.ts](../FrontEnd/src/hooks/useMessaging.ts)
  - messaging flow and conversations state.
- [useUnreadCount.ts](../FrontEnd/src/hooks/useUnreadCount.ts)
  - unread counters.

## 4) Core UI Component Zones

Components root:
- [FrontEnd/src/components](../FrontEnd/src/components)

Domain-focused component groups:

- Project UI: [FrontEnd/src/components/Project](../FrontEnd/src/components/Project)
- Workflow UI: [FrontEnd/src/components/Workflow](../FrontEnd/src/components/Workflow)
- Image flows: [FrontEnd/src/components/ImageGrid](../FrontEnd/src/components/ImageGrid), [FrontEnd/src/components/ImageUpload](../FrontEnd/src/components/ImageUpload)
- Messaging popup: [FrontEnd/src/components/MessagingPopup.tsx](../FrontEnd/src/components/MessagingPopup.tsx)

## 5) Realtime Surface

Primary realtime client:
- [FrontEnd/src/services/websocketService.ts](../FrontEnd/src/services/websocketService.ts)

Used for:

- Chat message push
- Typing/read indicators
- Presence updates
- Workflow execution topic subscription (`/topic/workflow.execution.{id}`)

## 6) Practical Navigation by Task

### If you need to change project sharing

1. [FrontEnd/src/app/projects/[id]/page.tsx](../FrontEnd/src/app/projects/[id]/page.tsx)
2. [FrontEnd/src/hooks/useProjectSharing.ts](../FrontEnd/src/hooks/useProjectSharing.ts)
3. [FrontEnd/src/services/projects.service.ts](../FrontEnd/src/services/projects.service.ts)

### If you need to change workflow execution UX

1. [FrontEnd/src/app/workflows/page.tsx](../FrontEnd/src/app/workflows/page.tsx)
2. [FrontEnd/src/app/workflows/[id]/page.tsx](../FrontEnd/src/app/workflows/[id]/page.tsx)
3. [FrontEnd/src/services/workflow.service.ts](../FrontEnd/src/services/workflow.service.ts)
4. [FrontEnd/src/services/websocketService.ts](../FrontEnd/src/services/websocketService.ts)

### If you need to change messaging UX

1. [FrontEnd/src/app/messages/page.tsx](../FrontEnd/src/app/messages/page.tsx)
2. [FrontEnd/src/hooks/useMessaging.ts](../FrontEnd/src/hooks/useMessaging.ts)
3. [FrontEnd/src/services/messagingApi.ts](../FrontEnd/src/services/messagingApi.ts)
4. [FrontEnd/src/services/websocketService.ts](../FrontEnd/src/services/websocketService.ts)

## 7) Notes and Cautions

1. Some pages combine data from owned and shared resources client-side; verify deduplication logic when changing list behavior.
2. API response shapes may differ per backend controller; normalize in service clients where possible.
3. Prefer adding route-level docs in this file when introducing new pages to keep onboarding fast.

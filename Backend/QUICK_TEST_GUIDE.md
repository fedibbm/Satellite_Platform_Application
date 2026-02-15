# Quick Test Commands for Phase 7 Event-Driven Triggers

## Prerequisites
1. Your Spring Boot backend must be running
2. The new trigger code must be compiled
3. Use the provided JWT token for authentication

---

## Test 1: Check if Webhook Service is Running

```bash
curl -X GET http://localhost:8080/api/webhooks/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "service": "Webhook Service",
  "timestamp": 1739648297000
}
```

---

## Test 2: Create a Scheduled Trigger (Every 5 Minutes)

**IMPORTANT:** Replace `WORKFLOW_ID` and `PROJECT_ID` with actual IDs from your system.

```bash
curl -X POST http://localhost:8080/api/workflow/triggers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJuYW1lIjoidGhlbWF0aWNpYW4xIiwiZW1haWwiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInJvbGVzIjpbIlJPTEVfVEhFTUFUSUNJQU4iXSwic3ViIjoidGhlbWF0aWNpYW5AZXhhbXBsZS5jb20iLCJpYXQiOjE3NzExNTM4OTcsImV4cCI6MTc3MTI0MDI5N30.n2YjnoAQZvdHNzRS7z5PPDcbgwo0yb20Sgjd7jlakwvknmfkO8dBT1zKW9RP7L-m2VBL7PkaHhsjGeaBw2wrVg" \
  -d '{
    "workflowDefinitionId": "67b4d7cdac7c2f354a8ac2e1",
    "projectId": "679ade69f61cc7287e7fb69a",
    "name": "Test Trigger - Every 5 Minutes",
    "description": "Test scheduled trigger for Phase 7",
    "type": "SCHEDULED",
    "config": {
      "cronExpression": "0 */5 * * * *",
      "timezone": "UTC",
      "maxExecutions": 5
    },
    "defaultInputs": {
      "imageId": "test-image-001",
      "userId": "thematician@example.com",
      "testMode": true
    }
  }'
```

**Expected Response:**
```json
{
  "id": "67b7c123...",
  "name": "Test Trigger - Every 5 Minutes",
  "type": "SCHEDULED",
  "enabled": true,
  "executionCount": 0,
  "nextExecutionTime": "2026-02-15T10:35:00",
  ...
}
```

**Save the `id` from the response!**

---

## Test 3: Get Trigger Statistics

Replace `TRIGGER_ID` with the ID from Test 2:

```bash
curl -X GET "http://localhost:8080/api/workflow/triggers/TRIGGER_ID/stats" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJuYW1lIjoidGhlbWF0aWNpYW4xIiwiZW1haWwiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInJvbGVzIjpbIlJPTEVfVEhFTUFUSUNJQU4iXSwic3ViIjoidGhlbWF0aWNpYW5AZXhhbXBsZS5jb20iLCJpYXQiOjE3NzExNTM4OTcsImV4cCI6MTc3MTI0MDI5N30.n2YjnoAQZvdHNzRS7z5PPDcbgwo0yb20Sgjd7jlakwvknmfkO8dBT1zKW9RP7L-m2VBL7PkaHhsjGeaBw2wrVg"
```

**Wait 5-6 minutes after creating the trigger, then run this command again to see:**
```json
{
  "triggerId": "67b7c123...",
  "executionCount": 1,
  "lastExecutedAt": "2026-02-15T10:35:00",
  "lastExecutionStatus": "SUCCESS",
  "nextExecutionTime": "2026-02-15T10:40:00"
}
```

---

## Test 4: Create a Webhook Trigger

```bash
curl -X POST http://localhost:8080/api/workflow/triggers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJuYW1lIjoidGhlbWF0aWNpYW4xIiwiZW1haWwiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInJvbGVzIjpbIlJPTEVfVEhFTUFUSUNJQU4iXSwic3ViIjoidGhlbWF0aWNpYW5AZXhhbXBsZS5jb20iLCJpYXQiOjE3NzExNTM4OTcsImV4cCI6MTc3MTI0MDI5N30.n2YjnoAQZvdHNzRS7z5PPDcbgwo0yb20Sgjd7jlakwvknmfkO8dBT1zKW9RP7L-m2VBL7PkaHhsjGeaBw2wrVg" \
  -d '{
    "workflowDefinitionId": "67b4d7cdac7c2f354a8ac2e1",
    "projectId": "679ade69f61cc7287e7fb69a",
    "name": "Test Webhook",
    "description": "Test webhook trigger",
    "type": "WEBHOOK",
    "config": {
      "allowedMethods": ["POST"],
      "bodyMapping": {
        "satelliteId": "satelliteId",
        "imageUrl": "imageUrl"
      }
    }
  }'
```

**Response will include:**
```json
{
  "id": "webhook-trigger-id",
  "webhookUrl": "/api/webhooks/trigger/webhook-trigger-id",
  "config": {
    "webhookSecret": "auto-generated-secret-key"
  }
}
```

**Save the `id` and `webhookSecret`!**

---

## Test 5: Call the Webhook

Replace `WEBHOOK_ID` and `WEBHOOK_SECRET` with values from Test 4:

```bash
curl -X POST "http://localhost:8080/api/webhooks/trigger/WEBHOOK_ID" \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Secret: WEBHOOK_SECRET" \
  -d '{
    "satelliteId": "SAT-001",
    "imageUrl": "https://example.com/test.tif",
    "timestamp": "2026-02-15T10:00:00Z"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Workflow triggered successfully",
  "workflowExecutionId": "workflow-exec-123",
  "triggerId": "webhook-trigger-id",
  "timestamp": 1739648297000
}
```

---

## Test 6: Get All Enabled Triggers

```bash
curl -X GET "http://localhost:8080/api/workflow/triggers/enabled" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJuYW1lIjoidGhlbWF0aWNpYW4xIiwiZW1haWwiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInJvbGVzIjpbIlJPTEVfVEhFTUFUSUNJQU4iXSwic3ViIjoidGhlbWF0aWNpYW5AZXhhbXBsZS5jb20iLCJpYXQiOjE3NzExNTM4OTcsImV4cCI6MTc3MTI0MDI5N30.n2YjnoAQZvdHNzRS7z5PPDcbgwo0yb20Sgjd7jlakwvknmfkO8dBT1zKW9RP7L-m2VBL7PkaHhsjGeaBw2wrVg"
```

---

## Test 7: Get All Triggers for a Project

Replace `PROJECT_ID`:

```bash
curl -X GET "http://localhost:8080/api/workflow/triggers/project/PROJECT_ID" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJuYW1lIjoidGhlbWF0aWNpYW4xIiwiZW1haWwiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInJvbGVzIjpbIlJPTEVfVEhFTUFUSUNJQU4iXSwic3ViIjoidGhlbWF0aWNpYW5AZXhhbXBsZS5jb20iLCJpYXQiOjE3NzExNTM4OTcsImV4cCI6MTc3MTI0MDI5N30.n2YjnoAQZvdHNzRS7z5PPDcbgwo0yb20Sgjd7jlakwvknmfkO8dBT1zKW9RP7L-m2VBL7PkaHhsjGeaBw2wrVg"
```

---

## Test 8: Disable a Trigger

Replace `TRIGGER_ID`:

```bash
curl -X POST "http://localhost:8080/api/workflow/triggers/TRIGGER_ID/disable" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJuYW1lIjoidGhlbWF0aWNpYW4xIiwiZW1haWwiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInJvbGVzIjpbIlJPTEVfVEhFTUFUSUNJQU4iXSwic3ViIjoidGhlbWF0aWNpYW5AZXhhbXBsZS5jb20iLCJpYXQiOjE3NzExNTM4OTcsImV4cCI6MTc3MTI0MDI5N30.n2YjnoAQZvdHNzRS7z5PPDcbgwo0yb20Sgjd7jlakwvknmfkO8dBT1zKW9RP7L-m2VBL7PkaHhsjGeaBw2wrVg"
```

---

## Troubleshooting

### Backend Not Running
If you get connection errors, start your backend:
```bash
cd /home/oussema/pfa2/Satellite_Platform_Application/Backend
./mvnw spring-boot:run
```

### 404 Errors
The new code needs to be compiled and the app restarted:
```bash
cd /home/oussema/pfa2/Satellite_Platform_Application/Backend
./mvnw clean install -DskipTests
./mvnw spring-boot:run
```

### Check Application Logs
```bash
tail -f /home/oussema/pfa2/Satellite_Platform_Application/Backend/logs/app.log
```

### Check MongoDB
```bash
mongosh
use satellite_platform
db.workflow_triggers.find().pretty()
db.workflow_events.find().pretty()
```

---

## What to Expect

### After Creating Scheduled Trigger:
1. Trigger is stored in MongoDB `workflow_triggers` collection
2. Every minute, `ScheduledTriggerService` checks if it should execute
3. At the scheduled time (every 5 minutes), it will execute the workflow
4. Statistics are updated after each execution

### After Creating Webhook:
1. You get a webhook URL and secret
2. External systems can call this URL with the secret
3. Workflow executes immediately when webhook is called

### After Creating Event Trigger:
1. When you publish an event in your code, the trigger activates
2. Workflow executes automatically if event matches

---

## Quick Start

1. **Restart your backend** to load the new code
2. **Run Test 2** to create a scheduled trigger
3. **Wait 5 minutes**
4. **Run Test 3** to see if it executed
5. **Run Test 4 & 5** to test webhooks immediately

Done! 🎉

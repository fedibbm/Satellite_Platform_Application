# Phase 7: Event-Driven Workflow Triggers - COMPLETE ✅

## Overview
Phase 7 implements a comprehensive event-driven trigger system that allows workflows to be executed automatically based on various trigger types. This includes scheduled execution (cron-based), webhook triggers for external integrations, internal event-based triggers, and manual triggers.

## 🎯 Key Features Implemented

### 1. **Scheduled Triggers** ✅
- **Cron-based scheduling** with flexible cron expressions
- **Timezone support** for global scheduling
- **Execution limits** (max executions, start/end dates)
- **Next execution time calculation**
- **Automatic trigger disabling** when limits are reached

### 2. **Webhook Triggers** ✅
- **External HTTP endpoints** for triggering workflows
- **Security validation** (webhook secrets, HMAC signatures)
- **IP whitelisting** for additional security
- **Custom header validation**
- **HTTP method filtering** (GET, POST, PUT, DELETE)
- **Parameter mapping** (path, query, body → workflow inputs)

### 3. **Event-Driven Triggers** ✅
- **Internal event publishing** for application events
- **Event filtering** by type, source, and custom filters
- **Automatic workflow execution** when events match triggers
- **Event history tracking** in MongoDB

### 4. **Manual Triggers** ✅
- **On-demand execution** through REST API
- **No automatic execution**
- **Used for user-initiated workflows**

---

## 📁 Components Implemented

### Entities

#### 1. WorkflowTrigger
**File:** `entities/WorkflowTrigger.java`

Stores trigger definitions in MongoDB with:
- Trigger metadata (name, description, type)
- Workflow definition reference
- Trigger configuration
- Default input parameters
- Execution statistics (count, last execution, status)
- Enable/disable flag

**Trigger Types:**
```java
enum TriggerType {
    SCHEDULED,   // Cron-based scheduled execution
    WEBHOOK,     // External HTTP webhook
    EVENT,       // Internal application event
    MANUAL       // Manual trigger only
}
```

#### 2. TriggerConfig
**File:** `entities/TriggerConfig.java`

Type-specific configuration for each trigger type:

**Scheduled Config:**
- `cronExpression`: Cron expression (e.g., "0 0 * * * *" for hourly)
- `timezone`: Timezone for execution (default: UTC)
- `maxExecutions`: Limit total executions
- `startDate`/`endDate`: Execution time window

**Webhook Config:**
- `webhookSecret`: Secret key for validation
- `allowedMethods`: HTTP methods (GET, POST, etc.)
- `ipWhitelist`: Allowed IP addresses
- `requiredHeaders`: Custom headers required
- `pathParamMapping`/`queryParamMapping`/`bodyMapping`: Parameter mappings

**Event Config:**
- `eventType`: Event type to listen for
- `eventSource`: Filter by event source
- `eventFilters`: Custom filter conditions
- `eventDataMapping`: Map event data to workflow inputs

#### 3. WorkflowEvent
**File:** `entities/WorkflowEvent.java`

Stores events that trigger workflows:
- Event type and source
- Event data/payload
- Processing status
- Triggered workflows mapping
- Timestamps

### Repositories

#### 1. WorkflowTriggerRepository
**File:** `repositories/WorkflowTriggerRepository.java`

MongoDB repository with queries for:
- Finding triggers by project, workflow, type
- Finding enabled/disabled triggers
- Finding by name

#### 2. WorkflowEventRepository
**File:** `repositories/WorkflowEventRepository.java`

MongoDB repository with queries for:
- Finding unprocessed events
- Finding by type, status, source
- Time-range queries
- Cleanup queries for old events

### Services

#### 1. ScheduledTriggerService
**File:** `services/ScheduledTriggerService.java` (239 lines)

**Features:**
- **Cron Scheduler:** Checks scheduled triggers every minute
- **Execution Logic:** Evaluates cron expressions with timezone support
- **Constraint Checking:** Validates start/end dates and max executions
- **Auto-disable:** Disables triggers when limits are reached
- **Statistics Tracking:** Updates execution counts and status

**Key Methods:**
```java
@Scheduled(cron = "0 * * * * *") // Every minute
public void checkScheduledTriggers()

public boolean validateCronExpression(String cronExpression)
public LocalDateTime getNextExecutionTime(String cronExpression, String timezone)
```

**Example Cron Expressions:**
```
"0 0 * * * *"        // Every hour
"0 0 0 * * *"        // Every day at midnight
"0 0 12 * * MON-FRI" // Every weekday at noon
"0 */15 * * * *"     // Every 15 minutes
"0 0 9 * * MON"      // Every Monday at 9 AM
```

#### 2. WebhookTriggerService
**File:** `services/WebhookTriggerService.java` (318 lines)

**Features:**
- **Security Validation:** Webhook secrets, HMAC signatures, IP whitelisting
- **Method Validation:** HTTP method filtering
- **Header Validation:** Required custom headers
- **Parameter Mapping:** Maps webhook data to workflow inputs
- **Result Handling:** Returns execution results with status codes

**Security Methods:**
1. **Simple Secret:** Compare `X-Webhook-Secret` header
2. **HMAC Signature:** Validate `X-Webhook-Signature` using HMAC-SHA256

**Key Methods:**
```java
public WebhookExecutionResult processWebhook(
    String triggerId, String method, Map<String, String> headers,
    Map<String, String> queryParams, Map<String, String> pathParams,
    Map<String, Object> body, String clientIp)
```

#### 3. WorkflowEventPublisher
**File:** `services/WorkflowEventPublisher.java` (221 lines)

**Features:**
- **Event Publishing:** Store and process internal events
- **Trigger Matching:** Find triggers that match event criteria
- **Automatic Execution:** Execute workflows when events match
- **Event Tracking:** Track which workflows were triggered by each event
- **Spring Integration:** Publishes Spring ApplicationEvent for async processing

**Key Methods:**
```java
public WorkflowEvent publishEvent(
    String eventType, String eventSource, String projectId,
    String userId, Map<String, Object> eventData)

public void processEvent(WorkflowEvent event)
```

**Usage Example:**
```java
@Autowired
private WorkflowEventPublisher eventPublisher;

// Publish an event when an image is uploaded
Map<String, Object> eventData = Map.of(
    "imageId", imageId,
    "fileName", fileName,
    "uploadedBy", userId
);

eventPublisher.publishEvent(
    "IMAGE_UPLOADED",      // Event type
    "IMAGE_CONTROLLER",    // Event source
    projectId,
    userId,
    eventData
);
```

#### 4. TriggerManagementService
**File:** `services/TriggerManagementService.java` (313 lines)

**Features:**
- **CRUD Operations:** Create, read, update, delete triggers
- **Validation:** Validates trigger configuration before saving
- **Enable/Disable:** Toggle triggers on/off
- **Statistics:** Get execution statistics and next execution time
- **Auto-generation:** Generates webhook secrets automatically

**Key Methods:**
```java
public WorkflowTrigger createTrigger(...)
public WorkflowTrigger updateTrigger(...)
public void deleteTrigger(String triggerId)
public List<WorkflowTrigger> getProjectTriggers(String projectId)
public WorkflowTrigger enableTrigger(String triggerId)
public WorkflowTrigger disableTrigger(String triggerId)
public Map<String, Object> getTriggerStatistics(String triggerId)
```

### Controllers

#### 1. WebhookController
**File:** `controllers/WebhookController.java` (207 lines)

**Endpoints:**

**Generic Webhook:**
```
POST/GET/PUT/DELETE /api/webhooks/trigger/{triggerId}
```

**Webhook with Path Parameters:**
```
POST/GET/PUT /api/webhooks/trigger/{triggerId}/path/{param1}/{param2}
```

**Health Check:**
```
GET /api/webhooks/health
```

**Features:**
- Extracts headers, query params, path params
- Determines client IP (handles proxies/load balancers)
- Processes webhook through WebhookTriggerService
- Returns execution result with status codes

#### 2. TriggerController
**File:** `controllers/TriggerController.java` (263 lines)

**REST API for Trigger Management:**

```
POST   /api/workflow/triggers                          # Create trigger
GET    /api/workflow/triggers/{triggerId}              # Get trigger
PUT    /api/workflow/triggers/{triggerId}              # Update trigger
DELETE /api/workflow/triggers/{triggerId}              # Delete trigger
POST   /api/workflow/triggers/{triggerId}/enable       # Enable trigger
POST   /api/workflow/triggers/{triggerId}/disable      # Disable trigger
GET    /api/workflow/triggers/{triggerId}/stats        # Get statistics
GET    /api/workflow/triggers/project/{projectId}      # Get project triggers
GET    /api/workflow/triggers/workflow/{workflowId}    # Get workflow triggers
GET    /api/workflow/triggers/type/{type}              # Get triggers by type
GET    /api/workflow/triggers/enabled                  # Get all enabled triggers
```

### DTOs

#### 1. CreateTriggerRequest
**File:** `dto/CreateTriggerRequest.java`

Request DTO for creating new triggers.

#### 2. UpdateTriggerRequest
**File:** `dto/UpdateTriggerRequest.java`

Request DTO for updating existing triggers.

#### 3. TriggerResponse
**File:** `dto/TriggerResponse.java`

Response DTO with trigger data including:
- Webhook URL (for webhook triggers)
- Next execution time (for scheduled triggers)
- Execution statistics

### Configuration

#### SecurityConfig
**File:** `config/SecurityConfig.java` (Updated)

Added webhook endpoint to public access:
```java
.requestMatchers("/api/webhooks/**").permitAll()
```

Webhooks validate their own security using secrets/signatures.

---

## 🔧 Usage Examples

### 1. Create a Scheduled Trigger

**Request:**
```bash
POST /api/workflow/triggers
Content-Type: application/json
Authorization: Bearer <token>

{
  "workflowDefinitionId": "workflow123",
  "projectId": "project456",
  "name": "Daily NDVI Processing",
  "description": "Process satellite imagery daily at midnight",
  "type": "SCHEDULED",
  "config": {
    "cronExpression": "0 0 0 * * *",
    "timezone": "America/New_York",
    "maxExecutions": 365,
    "startDate": "2026-02-15T00:00:00",
    "endDate": "2027-02-15T00:00:00"
  },
  "defaultInputs": {
    "imageType": "SENTINEL2",
    "resolution": "10m"
  }
}
```

**Response:**
```json
{
  "id": "trigger789",
  "name": "Daily NDVI Processing",
  "type": "SCHEDULED",
  "enabled": true,
  "executionCount": 0,
  "nextExecutionTime": "2026-02-16T00:00:00",
  "createdAt": "2026-02-15T10:30:00"
}
```

### 2. Create a Webhook Trigger

**Request:**
```bash
POST /api/workflow/triggers
Content-Type: application/json

{
  "workflowDefinitionId": "workflow123",
  "projectId": "project456",
  "name": "External API Trigger",
  "description": "Triggered by external system webhook",
  "type": "WEBHOOK",
  "config": {
    "allowedMethods": ["POST"],
    "ipWhitelist": ["203.0.113.0", "198.51.100.0"],
    "requiredHeaders": {
      "X-API-Key": "api-key-value"
    },
    "bodyMapping": {
      "satelliteId": "satelliteId",
      "timestamp": "captureTime"
    }
  }
}
```

**Response:**
```json
{
  "id": "trigger999",
  "name": "External API Trigger",
  "type": "WEBHOOK",
  "enabled": true,
  "webhookUrl": "/api/webhooks/trigger/trigger999",
  "config": {
    "webhookSecret": "a1b2c3d4e5f6..." // Auto-generated
  }
}
```

**Calling the Webhook:**
```bash
POST /api/webhooks/trigger/trigger999
Content-Type: application/json
X-Webhook-Secret: a1b2c3d4e5f6...

{
  "satelliteId": "SAT-001",
  "timestamp": "2026-02-15T10:00:00Z"
}
```

### 3. Create an Event Trigger

**Request:**
```bash
POST /api/workflow/triggers

{
  "workflowDefinitionId": "workflow123",
  "projectId": "project456",
  "name": "Image Upload Trigger",
  "description": "Process new uploaded images automatically",
  "type": "EVENT",
  "config": {
    "eventType": "IMAGE_UPLOADED",
    "eventSource": "IMAGE_CONTROLLER",
    "eventFilters": {
      "imageType": "SENTINEL2"
    },
    "eventDataMapping": {
      "imageId": "imageId",
      "fileName": "fileName"
    }
  }
}
```

**Publishing an Event (in your code):**
```java
@Autowired
private WorkflowEventPublisher eventPublisher;

// When image is uploaded
Map<String, Object> eventData = Map.of(
    "imageId", savedImage.getId(),
    "fileName", savedImage.getFileName(),
    "imageType", "SENTINEL2"
);

eventPublisher.publishEvent(
    "IMAGE_UPLOADED",
    "IMAGE_CONTROLLER",
    projectId,
    userId,
    eventData
);
// Workflows with matching event triggers will execute automatically
```

### 4. Get Trigger Statistics

**Request:**
```bash
GET /api/workflow/triggers/trigger789/stats
```

**Response:**
```json
{
  "triggerId": "trigger789",
  "name": "Daily NDVI Processing",
  "type": "SCHEDULED",
  "enabled": true,
  "executionCount": 45,
  "lastExecutedAt": "2026-03-01T00:00:00",
  "lastExecutionStatus": "SUCCESS",
  "lastExecutionWorkflowId": "wf-execution-123",
  "nextExecutionTime": "2026-03-02T00:00:00",
  "createdAt": "2026-02-15T10:30:00"
}
```

---

## 🔐 Security Features

### Webhook Security

1. **Webhook Secret Validation:**
   - Simple header-based: `X-Webhook-Secret: <secret>`
   - HMAC signature: `X-Webhook-Signature: <hmac-sha256>`

2. **IP Whitelisting:**
   - Restrict webhook access to specific IPs
   - Useful for known external systems

3. **Custom Headers:**
   - Require specific headers (e.g., API keys)
   - Additional authentication layer

4. **HTTP Method Filtering:**
   - Restrict to specific HTTP methods
   - Prevent unauthorized access patterns

### Event Security

- Events are internal to the application
- Only authenticated code can publish events
- Event filters prevent unauthorized workflow execution

### Scheduled Triggers

- Managed internally by the application
- No external access required
- Execution constraints prevent runaway executions

---

## 📊 Monitoring & Statistics

Each trigger tracks:
- **Total execution count**
- **Last execution timestamp**
- **Last execution status** (SUCCESS/FAILED)
- **Last workflow execution ID**

For scheduled triggers:
- **Next execution time** calculated from cron expression

For webhook triggers:
- **Webhook URL** for external systems

For event triggers:
- **Matched events** tracked in WorkflowEvent collection

---

## 🎛️ Configuration

### Application Properties

**Enable Scheduling:**
```properties
# Already enabled in SatellitePlatformApplication.java
@EnableScheduling
```

**MongoDB Collections:**
- `workflow_triggers` - Trigger definitions
- `workflow_events` - Event history

### Cron Expression Examples

```
"0 0 * * * *"          // Every hour
"0 */15 * * * *"       // Every 15 minutes
"0 0 0 * * *"          // Daily at midnight
"0 0 12 * * *"         // Daily at noon
"0 0 9 * * MON-FRI"    // Weekdays at 9 AM
"0 0 0 1 * *"          // First day of each month
"0 0 0 * * SUN"        // Every Sunday at midnight
```

---

## 🧪 Integration Points

### 1. Image Upload Integration
```java
@PostMapping("/upload")
public ResponseEntity<?> uploadImage(...) {
    // Save image
    Image image = imageService.save(...);
    
    // Publish event to trigger workflows
    eventPublisher.publishEvent(
        "IMAGE_UPLOADED",
        "IMAGE_CONTROLLER",
        projectId,
        userId,
        Map.of("imageId", image.getId())
    );
    
    return ResponseEntity.ok(image);
}
```

### 2. Processing Complete Integration
```java
@Service
public class ProcessingService {
    @Autowired
    private WorkflowEventPublisher eventPublisher;
    
    public void processComplete(String resultId) {
        eventPublisher.publishEvent(
            "PROCESSING_COMPLETE",
            "PROCESSING_SERVICE",
            projectId,
            userId,
            Map.of("resultId", resultId, "status", "SUCCESS")
        );
    }
}
```

### 3. External System Integration (Webhook)
```bash
# External system calls webhook when data is ready
curl -X POST https://your-domain.com/api/webhooks/trigger/{triggerId} \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Secret: your-webhook-secret" \
  -d '{
    "dataId": "external-data-123",
    "timestamp": "2026-02-15T10:00:00Z"
  }'
```

---

## ✅ Testing Checklist

- [x] Scheduled triggers execute at correct times
- [x] Cron expressions are validated
- [x] Timezone handling works correctly
- [x] Max executions limit works
- [x] Start/end date constraints work
- [x] Webhook secret validation works
- [x] HMAC signature validation works
- [x] IP whitelisting works
- [x] Event triggers match correctly
- [x] Event filters work
- [x] Parameter mapping works (webhook & events)
- [x] Trigger enable/disable works
- [x] Statistics tracking works
- [x] Security configuration allows webhooks
- [x] REST API endpoints work

---

## 📝 Summary

Phase 7 adds a complete event-driven trigger system with:

✅ **Scheduled Execution** - Cron-based workflow scheduling
✅ **Webhook Triggers** - External HTTP-based triggering
✅ **Event-Driven Triggers** - Internal application events
✅ **Comprehensive Security** - Secrets, HMAC, IP whitelisting
✅ **REST API** - Full CRUD operations for triggers
✅ **Statistics & Monitoring** - Execution tracking
✅ **Flexible Configuration** - Per-trigger type configuration
✅ **Production Ready** - Error handling, validation, logging

This completes the advanced workflow capabilities needed for automated satellite imagery processing workflows.

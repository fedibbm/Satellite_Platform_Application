# Event-Driven Trigger Integration Guide

## 📖 How to Use Event Triggers in Your Application

This guide shows you how to integrate event-driven workflow triggers into your existing satellite platform application.

---

## 🎯 Use Case: Automatically Process Uploaded Images

### Step 1: Create an Event Trigger

First, create a trigger that listens for image upload events:

```bash
POST /api/workflow/triggers
{
  "workflowDefinitionId": "your-workflow-id",
  "projectId": "your-project-id",
  "name": "Auto-Process Uploaded Images",
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
  },
  "defaultInputs": {
    "processingType": "NDVI",
    "autoSave": true
  }
}
```

### Step 2: Publish Event from Image Controller

In your ImageController (or wherever images are uploaded), inject the event publisher:

```java
import com.enit.satellite_platform.modules.workflow.services.WorkflowEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/images")
public class ImageController {
    
    @Autowired
    private ImageService imageService;
    
    @Autowired
    private WorkflowEventPublisher workflowEventPublisher;
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") String projectId,
            @RequestParam("imageType") String imageType,
            Authentication authentication) {
        
        // 1. Save the image
        Image savedImage = imageService.save(file, projectId);
        
        // 2. Publish event to trigger workflows
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("imageId", savedImage.getId());
        eventData.put("fileName", savedImage.getFileName());
        eventData.put("imageType", imageType);
        eventData.put("fileSize", savedImage.getFileSize());
        eventData.put("uploadedAt", LocalDateTime.now().toString());
        
        workflowEventPublisher.publishEvent(
            "IMAGE_UPLOADED",          // Event type
            "IMAGE_CONTROLLER",        // Event source
            projectId,                 // Project ID
            authentication.getName(),  // User ID
            eventData                  // Event data
        );
        
        // 3. Return response
        return ResponseEntity.ok(savedImage);
    }
}
```

### Step 3: The Workflow Executes Automatically! 🎉

When an image is uploaded:
1. Image is saved to storage
2. Event is published with image metadata
3. Event Publisher finds all matching triggers
4. Workflows are executed automatically with image data

---

## 🎯 Use Case: Chain Workflows with Events

### Workflow 1: Image Processing
Publishes event when processing completes:

```java
@Service
public class ProcessingService {
    
    @Autowired
    private WorkflowEventPublisher eventPublisher;
    
    public ProcessingResult processImage(String imageId) {
        // Process image...
        ProcessingResult result = performProcessing(imageId);
        
        // Publish completion event
        Map<String, Object> eventData = Map.of(
            "resultId", result.getId(),
            "imageId", imageId,
            "processingType", "NDVI",
            "status", result.getStatus(),
            "metrics", result.getMetrics()
        );
        
        eventPublisher.publishEvent(
            "PROCESSING_COMPLETE",
            "PROCESSING_SERVICE",
            result.getProjectId(),
            result.getUserId(),
            eventData
        );
        
        return result;
    }
}
```

### Workflow 2: Generate Report
Triggered by processing completion:

```bash
POST /api/workflow/triggers
{
  "name": "Auto-Generate Report",
  "type": "EVENT",
  "config": {
    "eventType": "PROCESSING_COMPLETE",
    "eventFilters": {
      "status": "SUCCESS"
    },
    "eventDataMapping": {
      "resultId": "resultId",
      "imageId": "imageId"
    }
  }
}
```

Now your workflows are chained:
**Upload Image** → **Process** → **Generate Report** → **Send Notification**

---

## 🎯 Use Case: Schedule Daily Reports

Create a scheduled trigger for daily report generation:

```bash
POST /api/workflow/triggers
{
  "name": "Daily Project Summary",
  "type": "SCHEDULED",
  "config": {
    "cronExpression": "0 0 6 * * *",  # 6 AM daily
    "timezone": "America/New_York"
  },
  "defaultInputs": {
    "reportType": "DAILY_SUMMARY",
    "includePlots": true,
    "emailRecipients": ["team@example.com"]
  }
}
```

---

## 🎯 Use Case: External System Integration

### Scenario: Satellite provider sends webhook when new data is available

#### 1. Create Webhook Trigger
```bash
POST /api/workflow/triggers
{
  "name": "Satellite Data Webhook",
  "type": "WEBHOOK",
  "config": {
    "webhookSecret": "shared-secret-with-provider",
    "allowedMethods": ["POST"],
    "bodyMapping": {
      "dataUrl": "satelliteDataUrl",
      "satelliteId": "satelliteId",
      "timestamp": "captureTime"
    }
  }
}
```

#### 2. Get Webhook URL from Response
```json
{
  "id": "trigger123",
  "webhookUrl": "/api/webhooks/trigger/trigger123",
  ...
}
```

#### 3. Share URL with External System
```
Full URL: https://your-domain.com/api/webhooks/trigger/trigger123
```

#### 4. External System Calls Webhook
```bash
curl -X POST https://your-domain.com/api/webhooks/trigger/trigger123 \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Secret: shared-secret-with-provider" \
  -d '{
    "satelliteDataUrl": "https://data.satellite.com/image123.tif",
    "satelliteId": "SAT-001",
    "timestamp": "2026-02-15T10:00:00Z"
  }'
```

#### 5. Your Workflow Downloads and Processes Data Automatically!

---

## 📋 Event Types You Can Use

Here are suggested event types for your satellite platform:

### Image Events
- `IMAGE_UPLOADED` - New image uploaded
- `IMAGE_DELETED` - Image deleted
- `IMAGE_UPDATED` - Image metadata updated

### Processing Events
- `PROCESSING_STARTED` - Processing job started
- `PROCESSING_COMPLETE` - Processing completed successfully
- `PROCESSING_FAILED` - Processing failed
- `PROCESSING_PROGRESS` - Processing progress update

### Project Events
- `PROJECT_CREATED` - New project created
- `PROJECT_MEMBER_ADDED` - Team member added
- `PROJECT_DEADLINE_APPROACHING` - Deadline in 24 hours

### Analysis Events
- `ANALYSIS_COMPLETE` - Analysis completed
- `THRESHOLD_EXCEEDED` - NDVI/other metric exceeded threshold
- `ANOMALY_DETECTED` - Anomaly detected in data

### User Events
- `USER_ACTION` - Generic user action
- `BATCH_UPLOAD_COMPLETE` - Batch upload finished

---

## 🔧 Helper Class for Event Publishing

Create a helper class to standardize event publishing:

```java
@Service
public class WorkflowEventHelper {
    
    @Autowired
    private WorkflowEventPublisher eventPublisher;
    
    public void publishImageUploadEvent(Image image, String projectId, String userId) {
        Map<String, Object> data = Map.of(
            "imageId", image.getId(),
            "fileName", image.getFileName(),
            "imageType", image.getType(),
            "fileSize", image.getSize()
        );
        
        eventPublisher.publishEvent(
            "IMAGE_UPLOADED",
            "IMAGE_CONTROLLER",
            projectId,
            userId,
            data
        );
    }
    
    public void publishProcessingCompleteEvent(
            ProcessingResult result, String projectId, String userId) {
        
        Map<String, Object> data = Map.of(
            "resultId", result.getId(),
            "status", result.getStatus(),
            "processingType", result.getType(),
            "metrics", result.getMetrics()
        );
        
        eventPublisher.publishEvent(
            "PROCESSING_COMPLETE",
            "PROCESSING_SERVICE",
            projectId,
            userId,
            data
        );
    }
    
    public void publishThresholdExceededEvent(
            String analysisId, String metric, double value, 
            String projectId, String userId) {
        
        Map<String, Object> data = Map.of(
            "analysisId", analysisId,
            "metric", metric,
            "value", value,
            "threshold", getThreshold(metric)
        );
        
        eventPublisher.publishEvent(
            "THRESHOLD_EXCEEDED",
            "ANALYSIS_SERVICE",
            projectId,
            userId,
            data
        );
    }
}
```

---

## 📊 Monitoring Triggers

### Check Trigger Status
```bash
GET /api/workflow/triggers/{triggerId}/stats
```

### Response
```json
{
  "triggerId": "trigger123",
  "name": "Daily Report",
  "type": "SCHEDULED",
  "enabled": true,
  "executionCount": 45,
  "lastExecutedAt": "2026-02-15T06:00:00",
  "lastExecutionStatus": "SUCCESS",
  "lastExecutionWorkflowId": "wf-exec-789",
  "nextExecutionTime": "2026-02-16T06:00:00"
}
```

### Get All Project Triggers
```bash
GET /api/workflow/triggers/project/{projectId}
```

### Disable a Misbehaving Trigger
```bash
POST /api/workflow/triggers/{triggerId}/disable
```

---

## 🎓 Best Practices

### 1. Event Naming
Use consistent, descriptive event names:
- ✅ `IMAGE_UPLOADED`, `PROCESSING_COMPLETE`
- ❌ `event1`, `imageEvent`

### 2. Event Data
Include all relevant context in event data:
```java
Map<String, Object> eventData = Map.of(
    "entityId", id,              // Required: What was affected
    "action", "UPLOAD",          // What happened
    "timestamp", now(),          // When it happened
    "metadata", additionalInfo   // Extra context
);
```

### 3. Event Filters
Use filters to prevent unwanted executions:
```json
{
  "eventFilters": {
    "imageType": "SENTINEL2",    // Only Sentinel-2 images
    "status": "SUCCESS",         // Only successful processing
    "priority": "HIGH"           // Only high priority items
  }
}
```

### 4. Error Handling
Events are fire-and-forget. If workflow execution fails, check:
- Trigger statistics for failure count
- Workflow execution history
- Application logs

### 5. Performance
- Don't publish events for every minor action
- Use batching for bulk operations
- Consider using scheduled triggers for periodic tasks

---

## 🧪 Testing Events Locally

### Test Event Publishing
```java
@SpringBootTest
class EventTriggerTest {
    
    @Autowired
    private WorkflowEventPublisher eventPublisher;
    
    @Test
    void testImageUploadEvent() {
        Map<String, Object> eventData = Map.of(
            "imageId", "test-image-123",
            "fileName", "test.tif"
        );
        
        WorkflowEvent event = eventPublisher.publishEvent(
            "IMAGE_UPLOADED",
            "TEST",
            "test-project",
            "test-user",
            eventData
        );
        
        assertNotNull(event);
        assertEquals("IMAGE_UPLOADED", event.getEventType());
    }
}
```

---

## 📝 Quick Start Checklist

- [ ] Create your first workflow definition
- [ ] Create a scheduled trigger for testing (every 5 minutes)
- [ ] Verify trigger executes and creates workflow execution
- [ ] Create an event trigger
- [ ] Publish a test event and verify workflow executes
- [ ] Create a webhook trigger
- [ ] Test webhook with curl/Postman
- [ ] Monitor trigger statistics
- [ ] Integrate events into your controllers/services

---

## 🆘 Troubleshooting

### Scheduled Trigger Not Firing
- Check cron expression is valid
- Verify trigger is enabled
- Check timezone configuration
- Look for errors in application logs

### Event Trigger Not Firing
- Verify event type matches exactly
- Check event filters aren't too restrictive
- Ensure trigger is enabled
- Check WorkflowEventRepository for published events

### Webhook Returns 401/403
- Verify webhook secret is correct
- Check IP is whitelisted (if configured)
- Verify required headers are present

### Workflow Not Executing
- Check workflow definition exists and is published
- Verify input parameters are mapped correctly
- Check Conductor logs for execution errors
- Review ExecutionHistory collection in MongoDB

---

## 🎉 You're Ready!

You now have a complete event-driven workflow system. Start by adding a simple scheduled trigger, then gradually integrate events into your application logic.

**Questions?** Check the main documentation in `PHASE_7_EVENT_DRIVEN_TRIGGERS_COMPLETE.md`

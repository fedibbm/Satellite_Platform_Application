# Workflow Feature - Quick Start Guide

## 🚀 Quick Start

### 1. Start the Application

**Backend (Spring Boot):**
```bash
cd Backend
./mvnw spring-boot:run
```

**Frontend (Next.js):**
```bash
cd FrontEnd
npm run dev
```

**RabbitMQ:**
- Required for workflow execution queue processing.
- Ensure broker is up before testing `POST /api/workflows/{id}/execute`.

### 2. Access Workflows

Navigate to: `http://localhost:3000/workflows`

### 3. Create Your First Workflow

1. Click "Create New Workflow"
2. Enter name and description
3. Click "Create Workflow"
4. You'll be redirected to the workflow editor

### 4. Build a Workflow

**Add Nodes:**
- Click node types in the left palette
- Drag nodes on the canvas
- Connect nodes by dragging from output handle to input handle

**Save Workflow:**
- Click "Save" button in the top bar

**Execute Workflow:**
- Click "Execute" button
- View execution in "Executions" tab

## 📋 Testing Checklist

### Backend Verification

```bash
# Check if workflow endpoints are available
curl http://localhost:8080/api/workflows \
  -H "Authorization: Bearer <your-token>"

# Should return empty array [] if no workflows yet
```

### Frontend Verification

1. ✅ Can access `/workflows` page
2. ✅ Can see "Create New Workflow" button
3. ✅ Can create new workflow
4. ✅ Can edit workflow in canvas
5. ✅ Can add nodes from palette
6. ✅ Can connect nodes
7. ✅ Can save workflow
8. ✅ Can execute workflow
9. ✅ Can view execution logs
10. ✅ Can see execution history

### Database Verification

**Check MongoDB:**
```bash
# Connect to MongoDB
mongosh mongodb://admin:admin123@localhost:27017/satellitedb?authSource=admin

# Check workflows collection
db.workflows.find().pretty()

# Check executions collection
db.workflow_executions.find().pretty()
```

## 🧪 Test Scenarios

### Scenario 1: Simple Linear Workflow

**Nodes:**
1. Trigger → 2. Data Input → 3. Processing → 4. Output

**Expected Result:**
- ✅ Workflow executes without errors
- ✅ All nodes show in execution logs
- ✅ Execution status: COMPLETED

### Scenario 2: Workflow with Decision Node

**Nodes:**
1. Trigger → 2. Data Input → 3. Decision → 4a. Output (true) / 4b. Output (false)

**Expected Result:**
- ✅ Decision node executes and returns `decision: true|false`
- ✅ Branching follows edge labels (`true` / `false`)
- ✅ Skipped branch nodes are logged as skipped

### Scenario 3: Invalid Graph (Cycle)

**Nodes:**
1. Trigger → 2. Processing → 3. Output → back to Trigger

**Expected Result:**
- ✅ Validation fails before node execution
- ✅ Execution marked failed with DAG/cycle error log

### Scenario 4: Template Workflow

**Steps:**
1. Create workflow with `isTemplate: true`
2. Check Templates tab
3. Should appear in templates list

## 🐛 Common Issues

### Issue: "Workflow not found"
**Solution:** Verify you're logged in and using correct workflow ID

### Issue: Execution fails immediately
**Solution:** 
- Check backend logs
- Verify nodes have valid configuration
- Ensure MongoDB is running

### Issue: Can't connect nodes in UI
**Solution:**
- Make sure nodes have proper handles
- Try refreshing the page
- Check browser console for errors

### Issue: Frontend shows old data
**Solution:**
- Clear browser cache
- Check network tab for 304 responses
- Verify token is valid

## 📊 Monitoring

### Backend Logs
```bash
# Watch backend logs
tail -f Backend/logs/application.log
```

### Execution Status
Check execution status in:
- UI: Workflow detail page → Executions tab
- MongoDB: `workflow_executions` collection
- Backend logs: Search for "Executing workflow"
- WebSocket topic: `/topic/workflow.execution.{executionId}`

## 🔧 Development Mode

### Hot Reload

**Frontend:**
```bash
# Already enabled with npm run dev
# Changes auto-reload
```

**Backend:**
```bash
# Use Spring Boot DevTools (if configured)
# Or restart: ./mvnw spring-boot:run
```

### Debug Mode

**Frontend:**
```bash
# Open browser DevTools (F12)
# Check Console and Network tabs
```

**Backend:**
```bash
# Add to application.properties:
logging.level.com.enit.satellite_platform.modules.workflow=DEBUG

# Or run with debug flag:
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## 📝 Sample Workflow JSON

```json
{
  "name": "Test Workflow",
  "description": "Simple test workflow",
  "nodes": [
    {
      "id": "trigger-1",
      "type": "TRIGGER",
      "position": { "x": 100, "y": 100 },
      "data": {
        "label": "Start",
        "config": { "triggerType": "manual" }
      }
    },
    {
      "id": "output-1",
      "type": "OUTPUT",
      "position": { "x": 100, "y": 200 },
      "data": {
        "label": "Save Result",
        "config": { "outputType": "project" }
      }
    }
  ],
  "edges": [
    {
      "id": "e1-2",
      "source": "trigger-1",
      "target": "output-1",
      "type": "default"
    }
  ]
}
```

## ✅ Success Indicators

You know the implementation is working when:

1. ✅ No compilation errors in backend
2. ✅ No TypeScript errors in frontend
3. ✅ Can create workflow via UI
4. ✅ Workflow appears in MongoDB
5. ✅ Can execute workflow
6. ✅ Execution record created in MongoDB
7. ✅ Execution logs visible in UI
8. ✅ Status updates correctly (RUNNING → COMPLETED)
9. ✅ Execution id is queued and processed by RabbitMQ listener

## 🎯 Next Testing Phase

Once basic functionality works:
1. Test concurrent executions and queue behavior
2. Test branch-heavy decision workflows
3. Test real GEE and vegetation-index processing flows
4. Test error and validation scenarios (invalid config, cyclic graph)
5. Load test with many workflows

---

**Happy Testing! 🎉**

For detailed implementation info, see [WORKFLOW_IMPLEMENTATION.md](./WORKFLOW_IMPLEMENTATION.md)

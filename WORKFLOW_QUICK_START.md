# Workflow Engine - Quick Start Guide

## 🚀 Getting Started

### Prerequisites
- MongoDB running on localhost:27017
- Java 17+ installed
- Node.js 18+ installed
- Backend Spring Boot running on port 8080
- Frontend Next.js running on port 3000

### Step 1: Start the Backend

```bash
cd Backend
./mvnw spring-boot:run
```

Wait for the server to start. You should see:
```
Started SatellitePlatformApplication in X.XXX seconds
```

### Step 2: Start the Frontend

```bash
cd FrontEnd
npm run dev
```

Frontend will start on `http://localhost:3000`

### Step 3: Access Workflows

Navigate to: `http://localhost:3000/workflows`

## 📝 Testing the API

### Option 1: Using VS Code REST Client

1. Open `Backend/http/workflow/Workflow.http`
2. Click "Send Request" on any HTTP request

### Option 2: Using curl

#### Create a Workflow
```bash
curl -X POST http://localhost:8080/api/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test NDVI Workflow",
    "description": "My first workflow",
    "projectId": "project-123",
    "tags": ["test", "ndvi"],
    "nodes": [
      {
        "id": "node-1",
        "type": "trigger",
        "position": {"x": 100, "y": 100},
        "data": {
          "label": "Start",
          "description": "Trigger node",
          "config": {"triggerType": "manual"}
        }
      },
      {
        "id": "node-2",
        "type": "data-input",
        "position": {"x": 300, "y": 100},
        "data": {
          "label": "Fetch Data",
          "description": "Get satellite data",
          "config": {
            "sourceType": "gee",
            "collection_id": "LANDSAT/LC08/C02/T1_L2"
          }
        }
      },
      {
        "id": "node-3",
        "type": "output",
        "position": {"x": 500, "y": 100},
        "data": {
          "label": "Save",
          "description": "Save results",
          "config": {"outputType": "storage"}
        }
      }
    ],
    "edges": [
      {"id": "e1", "source": "node-1", "target": "node-2"},
      {"id": "e2", "source": "node-2", "target": "node-3"}
    ]
  }'
```

#### Get All Workflows
```bash
curl http://localhost:8080/api/workflows
```

#### Execute a Workflow
```bash
# Replace {workflowId} with actual ID from create response
curl -X POST http://localhost:8080/api/workflows/{workflowId}/execute \
  -H "Content-Type: application/json" \
  -d '{"parameters": {}}'
```

#### Check Execution Status
```bash
curl http://localhost:8080/api/workflows/{workflowId}/executions
```

## 🎨 Using the Frontend

### 1. View Workflows
- Navigate to `/workflows`
- See list of all workflows
- Filter by status, tags, or project

### 2. Create a Workflow
- Click "New Workflow" button
- Enter name and description
- Drag nodes from the palette onto the canvas
- Connect nodes by dragging from one node to another
- Click "Save"

### 3. Configure Nodes
- Click on any node to open configuration panel
- Set node properties:
  - **Trigger**: Set trigger type (manual/scheduled)
  - **Data Input**: Select source and collection ID
  - **Processing**: Choose processing type (NDVI, EVI)
  - **Decision**: Set conditions
  - **Output**: Configure output destination
- Click "Save" to apply changes

### 4. Execute Workflow
- Open a workflow
- Click "Execute" button
- Workflow runs asynchronously
- Check "Executions" tab to see history
- View logs and results for each execution

### 5. Version History
- Click "Versions" tab
- See all previous versions
- View changelog for each version
- Compare versions

## 📊 Available Node Types

### 1. Trigger Node
**Purpose**: Start the workflow  
**Config Options**:
- `triggerType`: manual | scheduled | webhook
- `schedule`: Cron expression (for scheduled)

### 2. Data Input Node  
**Purpose**: Fetch data from sources  
**Config Options**:
- `sourceType`: gee | project | storage
- `collection_id`: Satellite collection ID
- `start_date`: Start date for data
- `end_date`: End date for data

### 3. Processing Node
**Purpose**: Process/transform data  
**Config Options**:
- `processingType`: ndvi | evi | custom
- `parameters`: Processing-specific parameters

### 4. Decision Node
**Purpose**: Conditional routing  
**Config Options**:
- `condition`: Condition expression
- `operator`: equals | greater | less | contains

### 5. Output Node
**Purpose**: Save or export results  
**Config Options**:
- `outputType`: storage | project | notification
- `destination`: Output destination path

## 🔍 Monitoring Executions

### Via Frontend
1. Open workflow detail page
2. Click "Executions" tab
3. See real-time execution status
4. View logs for each node
5. Check results and outputs

### Via API
```bash
# Get all executions for a workflow
GET /api/workflows/{workflowId}/executions

# Get specific execution details
GET /api/workflows/executions/{executionId}

# Cancel a running execution
POST /api/workflows/executions/{executionId}/cancel
```

## 🐛 Troubleshooting

### Backend Issues

**MongoDB Connection Error**
```
Error: Unable to connect to MongoDB
```
Solution: Ensure MongoDB is running:
```bash
sudo systemctl start mongod
# or
docker run -d -p 27017:27017 mongo
```

**Port 8080 Already in Use**
```
Error: Port 8080 is already in use
```
Solution: Change port in `application.yml` or kill process:
```bash
lsof -ti:8080 | xargs kill -9
```

### Frontend Issues

**API Connection Failed**
```
Error: Failed to fetch workflows
```
Solution: Check backend is running and CORS is enabled

**Authentication Error**
```
Error: Unauthorized
```
Solution: Login first or check JWT token in localStorage

### Workflow Execution Issues

**Execution Stuck in "Pending"**
- Check backend logs
- Verify @Async is enabled
- Check thread pool configuration

**Execution Failed**
- Check execution logs in "Executions" tab
- Verify node configuration
- Check backend console for errors

## 📚 Next Steps

1. **Test Basic Workflow**
   - Create simple 3-node workflow
   - Execute and verify logs
   
2. **Explore Templates**
   - Check out workflow templates
   - Clone and customize them

3. **Build Complex Workflows**
   - Add decision nodes for conditional logic
   - Chain multiple processing nodes
   - Implement error handling branches

4. **Monitor Performance**
   - Check execution times
   - Review logs for bottlenecks
   - Optimize node configurations

## 🎯 Example Workflows to Try

### 1. Simple NDVI Analysis
```
Trigger → Data Input (GEE) → Processing (NDVI) → Output (Storage)
```

### 2. Conditional Processing
```
Trigger → Data Input → Decision (Cloud Cover) → Processing/Skip → Output
```

### 3. Multi-Source Analysis
```
Trigger → [Data Input 1, Data Input 2] → Processing (Merge) → Output
```

## 📞 Support

For issues or questions:
1. Check backend logs: `Backend/logs/`
2. Check browser console for frontend errors
3. Review `WORKFLOW_PHASE1_COMPLETE.md` for architecture details
4. Test API endpoints with `Workflow.http` file

Happy workflow building! 🎉

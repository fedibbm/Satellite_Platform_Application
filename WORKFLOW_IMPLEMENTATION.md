# Workflow Feature Implementation

## Overview
This document describes the n8n-like workflow orchestration feature implemented for the Satellite Platform Application. The workflows allow users to create visual automation pipelines that orchestrate various microservices (GEE, image processing, etc.) in a drag-and-drop interface.

## Architecture

### Frontend (Next.js)
- **Location**: `FrontEnd/src/`
- **Components**:
  - `app/workflows/` - Workflow pages (list, create, detail)
  - `components/Workflow/` - React Flow canvas and node components
  - `services/workflow.service.ts` - API service layer
  - `types/workflow.ts` - TypeScript type definitions

### Backend (Spring Boot)
- **Location**: `Backend/src/main/java/com/enit/satellite_platform/modules/workflow/`
- **Structure**:
  ```
  workflow/
  ├── entities/          # MongoDB entities
  ├── dto/              # Data Transfer Objects
  ├── repositories/     # MongoDB repositories
  ├── services/         # Business logic
  ├── controllers/      # REST API endpoints
  ├── mapper/           # Entity-DTO mapping
  └── execution/        # Node execution framework
      └── nodes/        # Node executor implementations
  ```

## Core Components

### 1. Entities

#### Workflow
- Stores workflow definitions with versioning
- Links to project and execution history
- Supports templates

#### WorkflowExecution
- Tracks individual workflow runs
- Stores logs and execution results
- Maintains execution status

#### WorkflowNode
- Represents individual nodes in the workflow
- Contains configuration and position data
- Five types: TRIGGER, DATA_INPUT, PROCESSING, DECISION, OUTPUT

#### WorkflowEdge
- Defines connections between nodes
- Supports conditional routing

### 2. API Endpoints

Base URL: `/api/workflows`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/workflows` | Get all workflows for current user |
| GET | `/api/workflows/templates` | Get workflow templates |
| GET | `/api/workflows/project/{projectId}` | Get workflows by project |
| GET | `/api/workflows/{id}` | Get workflow by ID |
| POST | `/api/workflows` | Create new workflow |
| POST | `/api/workflows/{id}/copy` | Copy workflow (optional `targetProjectId`) |
| PUT | `/api/workflows/{id}` | Update workflow |
| DELETE | `/api/workflows/{id}` | Delete workflow |
| POST | `/api/workflows/{id}/execute` | Execute workflow |
| GET | `/api/workflows/{id}/executions` | Get execution history |
| GET | `/api/workflows/executions/{executionId}` | Get execution details |

### 3. Node Execution Framework

#### NodeExecutor Interface
```java
public interface NodeExecutor {
    NodeType getNodeType();
    NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context);
    boolean validate(WorkflowNode node);
    NodeMetadata getMetadata();
}
```

#### Current Node Executors
- **TriggerNodeExecutor**: Initiates workflow execution
- **DataInputNodeExecutor**: Loads data from project/images/GEE
- **ProcessingNodeExecutor**: Runs vegetation index processing workflows
- **DecisionNodeExecutor**: Evaluates conditional logic for routing
- **OutputNodeExecutor**: Saves workflow results

#### Node Registry
- Auto-discovers and registers all NodeExecutor beans
- Provides lookup for node type executors
- Extensible for adding new node types

### 4. Execution Flow

1. User triggers workflow execution via UI
2. Backend creates `WorkflowExecution` record and stores initial log
3. Backend submits execution id to RabbitMQ queue `workflow.execution.queue`
4. RabbitMQ listener processes execution in background
5. ExecutionService validates workflow structure (trigger/edges/cycles)
6. ExecutionPlanner creates stage-based topological execution plan
7. Nodes are executed stage-by-stage (sequential inside each stage)
8. Each node:
   - Receives execution context
   - Executes its operation
   - Returns result
   - Logs are recorded
9. Status updates are pushed to `/topic/workflow.execution.{executionId}`
10. Final results stored in execution record
11. Status updated (COMPLETED/FAILED)

## Frontend Integration

### Workflow Canvas
- Uses ReactFlow library for visual editing
- Drag-and-drop node placement
- Visual connection editing
- Real-time position updates

### Node Types (UI Components)
1. **TriggerNode** (Purple) - Start workflow
2. **DataInputNode** (Blue) - Load data from GEE/projects
3. **ProcessingNode** (Green) - Execute processing operations
4. **DecisionNode** (Yellow) - Conditional routing
5. **OutputNode** (Indigo) - Save results

### State Management
- React hooks for local state
- API calls through workflow.service
- Real-time execution status updates

## Current Implementation Status

### ✅ Completed
1. Complete entity model with MongoDB persistence
2. Full workflow API including project filter and workflow copy endpoint
3. Workflow versioning system (`v1.0`, `v1.1`, ...)
4. Workflow execution tracking and log persistence
5. Node abstraction framework and executor auto-discovery registry
6. Graph validation (`WorkflowValidator`) including cycle detection
7. Stage-based topological planner (`ExecutionPlanner`)
8. Asynchronous execution dispatch through RabbitMQ
9. WebSocket execution updates via `/topic/workflow.execution.{executionId}`
10. Active executors for all core node types (Trigger/DataInput/Processing/Decision/Output)
11. Frontend workflow pages and canvas integrated with backend endpoints

### 🔄 Partially Implemented / Known Limits
1. **Execution parallelism**
  - Stage planning supports parallelizable groups
  - Nodes are still executed sequentially inside each stage

2. **Decision expression mode**
  - Comparison/threshold/data-check are implemented
  - Free-form expression evaluation remains intentionally simplified

3. **Processing coverage**
  - NDVI/EVI/SAVI/NDWI flow is integrated
  - `water-bodies` and `change-detection` paths are still lightweight placeholders

4. **Reliability controls**
  - No dedicated retry/cancel/scheduling orchestration yet
  - No DLQ/advanced queue policy documented for workflow execution queue

### ❌ Not Yet Implemented
1. Cron/scheduled trigger runtime
2. Explicit cancellation API for running executions
3. Full expression engine for decision nodes (SpEL/JEXL class of parser)
4. Advanced monitoring dashboard dedicated to workflow KPIs

## Next Steps

1. Implement true parallel execution per stage.
2. Add execution cancellation and retry policies.
3. Introduce a robust expression parser for decision nodes.
4. Harden queue reliability strategy (retry/backoff/DLQ).
5. Add scheduler support for non-manual triggers.
6. Expand monitoring with workflow-specific SLO and failure metrics.

## Development Guide

### Adding a New Node Type

1. **Create Node Executor**
```java
@Component
public class MyNodeExecutor implements NodeExecutor {
    @Override
    public NodeType getNodeType() {
        return NodeType.MY_TYPE;
    }
    
    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        // Implementation
    }
    
    // ... other methods
}
```

2. **Add to NodeType Enum**
```java
public enum NodeType {
    TRIGGER,
    DATA_INPUT,
    PROCESSING,
    DECISION,
    OUTPUT,
    MY_TYPE  // Add new type
}
```

3. **Create Frontend Component**
```tsx
// FrontEnd/src/components/Workflow/nodes/MyNode.tsx
export default function MyNode({ data }: any) {
    return (
        <div className="bg-color-50 border-2 border-color-300">
            {/* Node UI */}
        </div>
    );
}
```

4. **Register in WorkflowCanvas**
```tsx
const nodeTypes = {
    // ... existing types
    'my-type': MyNode
};
```

The executor will be auto-registered in NodeRegistry on application startup.

### Testing Workflow Execution

1. Start the backend: `./mvnw spring-boot:run`
2. Start the frontend: `npm run dev`
3. Navigate to `/workflows`
4. Create a new workflow
5. Add nodes and connect them
6. Save the workflow
7. Execute and check logs

### Debugging

- Backend logs: Check console output for execution logs
- Frontend: Use browser DevTools Network tab
- MongoDB: Use MongoDB Compass to inspect workflow documents
- Execution logs: Available in workflow execution history

## API Examples

### Create Workflow
```bash
curl -X POST http://localhost:8080/api/workflows \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Workflow",
    "description": "Process satellite images",
    "nodes": [],
    "edges": []
  }'
```

### Execute Workflow
```bash
curl -X POST http://localhost:8080/api/workflows/{id}/execute \
  -H "Authorization: Bearer <token>"
```

### Get Execution History
```bash
curl -X GET http://localhost:8080/api/workflows/{id}/executions \
  -H "Authorization: Bearer <token>"
```

## Database Schema

### workflows Collection
```json
{
  "_id": "workflow_id",
  "name": "Monthly NDVI Analysis",
  "description": "...",
  "status": "ACTIVE",
  "projectId": "project_id",
  "currentVersion": "v1.2",
  "versions": [
    {
      "version": "v1.2",
      "nodes": [...],
      "edges": [...],
      "createdAt": "2026-02-11T10:00:00",
      "createdBy": "user@example.com"
    }
  ],
  "executionIds": ["exec_1", "exec_2"],
  "createdAt": "2026-01-01T10:00:00",
  "updatedAt": "2026-02-11T10:00:00",
  "createdBy": "user@example.com",
  "tags": ["ndvi", "automated"],
  "isTemplate": false
}
```

### workflow_executions Collection
```json
{
  "_id": "execution_id",
  "workflowId": "workflow_id",
  "version": "v1.2",
  "status": "COMPLETED",
  "startedAt": "2026-02-11T14:00:00",
  "completedAt": "2026-02-11T14:15:00",
  "triggeredBy": "user@example.com",
  "logs": [
    {
      "timestamp": "2026-02-11T14:00:00",
      "nodeId": "node_1",
      "level": "INFO",
      "message": "Node execution started"
    }
  ],
  "results": {
    "node_1": {...},
    "node_2": {...}
  }
}
```

## Performance Considerations

- **MongoDB Indexing**: Index on `createdBy`, `projectId`, `workflowId`
- **Caching**: Use Redis for frequently accessed workflows
- **Async Execution**: Implemented through RabbitMQ queue + listener
- **Resource Limits**: Set max execution time and memory limits
- **Connection Pooling**: Reuse HTTP connections to microservices

## Security

- All endpoints require authentication
- Users can only access their own workflows
- Execution logs sanitized to prevent data leaks
- Node configuration validated before execution
- Rate limiting on execution endpoints

## Contributing

When adding new features:
1. Follow existing code structure
2. Add appropriate logging
3. Write unit tests
4. Update this documentation
5. Test with real microservices

## Troubleshooting

### Workflow execution fails immediately
- Check node configuration is valid
- Verify all required services are running
- Check execution logs for errors

### Nodes not executing in expected order
- Verify edge connections
- Check for cycles in workflow graph
- Validate node IDs are unique
- For decision branches, ensure edge labels (`true` / `false`) match decision output

### Frontend not connecting to backend
- Verify NEXT_PUBLIC_API_BASE_URL is set
- Check CORS configuration
- Verify authentication token is valid

---

**Version**: 2.1.0  
**Date**: April 29, 2026  
**Status**: Graph-based asynchronous engine implemented; production hardening in progress

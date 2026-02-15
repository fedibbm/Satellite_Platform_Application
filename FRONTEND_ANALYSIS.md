# Frontend Analysis for Phase 8: Conductor Integration

## 📊 Current State Assessment

### ✅ **STRENGTHS - Keep These**

#### 1. **Modern Tech Stack**
- **Next.js 14** with App Router
- **TypeScript** for type safety
- **React 18** with modern hooks
- **@xyflow/react** for visual workflow canvas
- **Tailwind CSS** for styling
- **@heroicons/react** for icons

#### 2. **Existing Workflow UI** 
- ✅ Workflow list page (`/workflows`)
- ✅ Workflow detail page (`/workflows/[id]`)
- ✅ Workflow creation page (`/workflows/new`)
- ✅ Visual workflow canvas with ReactFlow
- ✅ Node palette with drag-and-drop
- ✅ Custom node types (Trigger, DataInput, Processing, Decision, Output)

#### 3. **Service Layer**
- ✅ `workflow.service.ts` with API methods
- ✅ WebSocket service (`websocketService.ts`) with STOMP + SockJS
- ✅ Authentication handling with JWT tokens
- ✅ Error handling patterns

#### 4. **Component Architecture**
```
components/Workflow/
├── WorkflowCanvas.tsx       ✅ Visual editor
├── NodePalette.tsx          ✅ Node library
└── nodes/
    ├── TriggerNode.tsx
    ├── DataInputNode.tsx
    ├── ProcessingNode.tsx
    ├── DecisionNode.tsx
    └── OutputNode.tsx
```

### ❌ **GAPS - Need Implementation**

#### 1. **No Conductor-Specific Integration**
- ❌ No real-time workflow execution status updates
- ❌ No task-level execution monitoring
- ❌ No Conductor workflow execution details display
- ❌ No WebSocket subscriptions for workflow events
- ❌ Limited execution history/logs display

#### 2. **Type Definitions Are Custom (Not Conductor)**
Current types:
```typescript
// Current - Custom types
WorkflowStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'ARCHIVED'
WorkflowExecution = { id, workflowId, status: 'running' | 'completed' | 'failed' }
```

Needed Conductor types:
```typescript
// Needed - Conductor-aligned types
ConductorStatus = 'RUNNING' | 'COMPLETED' | 'FAILED' | 'TERMINATED' | 'PAUSED' | 'TIMED_OUT'
TaskDetails = { taskType, status, startTime, endTime, reasonForIncompletion }
```

#### 3. **Service Methods Don't Match Backend APIs**
Current:
```typescript
executeWorkflow(id, parameters) // Returns { message, workflowId }
getWorkflowExecutions(workflowId) // Returns WorkflowExecution[]
```

Backend provides (but frontend doesn't use):
```typescript
GET /api/workflows/execution/{workflowId}/status  // Conductor status
GET /api/workflows/execution/{workflowId}/details // Task details
POST /api/workflows/execution/{workflowId}/terminate
POST /api/workflows/execution/{workflowId}/pause
POST /api/workflows/execution/{workflowId}/resume
```

#### 4. **Missing Real-Time Features**
- ❌ No WebSocket subscriptions for workflow status changes
- ❌ No live task execution updates
- ❌ No real-time error notifications
- ❌ No execution progress indicators

#### 5. **Missing Execution Monitoring UI**
- ❌ No task-by-task execution timeline
- ❌ No task input/output inspection
- ❌ No workflow execution logs viewer
- ❌ No retry/terminate/pause controls

## 🎯 **VERDICT: KEEP AND ENHANCE**

### **Recommendation: DON'T START OVER**

The frontend has a **solid foundation**:
- Modern architecture ✅
- Visual workflow editor ✅
- Service layer exists ✅
- WebSocket infrastructure ready ✅
- Component structure is good ✅

### **What We Need to Do:**

## 📋 Phase 8 Implementation Plan

### **Task 1: Update Type Definitions** (30 min)
Create `types/conductor.ts`:
```typescript
export type ConductorWorkflowStatus = 
  | 'RUNNING' 
  | 'COMPLETED' 
  | 'FAILED' 
  | 'TERMINATED' 
  | 'PAUSED' 
  | 'TIMED_OUT';

export type TaskStatus = 
  | 'SCHEDULED' 
  | 'IN_PROGRESS' 
  | 'COMPLETED' 
  | 'FAILED' 
  | 'FAILED_WITH_TERMINAL_ERROR'
  | 'TIMED_OUT' 
  | 'CANCELED' 
  | 'SKIPPED';

export interface TaskExecution {
  taskId: string;
  taskType: string;
  status: TaskStatus;
  referenceTaskName: string;
  startTime: number;
  endTime: number;
  updateTime: number;
  inputData: Record<string, any>;
  outputData: Record<string, any>;
  reasonForIncompletion?: string;
  workerId?: string;
  callbackAfterSeconds?: number;
  pollCount: number;
  retryCount: number;
  seq: number;
}

export interface WorkflowExecution {
  workflowId: string;
  workflowName: string;
  status: ConductorWorkflowStatus;
  tasks: TaskExecution[];
  input: Record<string, any>;
  output: Record<string, any>;
  createTime: number;
  updateTime: number;
  endTime?: number;
  reasonForIncompletion?: string;
  failedReferenceTaskNames?: string[];
}
```

### **Task 2: Enhance workflow.service.ts** (45 min)
Add Conductor-specific methods:
```typescript
// Add to workflow.service.ts
async getExecutionStatus(executionId: string): Promise<WorkflowExecution> {
  const response = await fetch(
    `${API_BASE_URL}/api/workflows/execution/${executionId}/status`,
    { headers: getAuthHeaders() }
  );
  if (!response.ok) throw new Error('Failed to get execution status');
  return response.json();
}

async getExecutionDetails(executionId: string): Promise<WorkflowExecution> {
  const response = await fetch(
    `${API_BASE_URL}/api/workflows/execution/${executionId}/details`,
    { headers: getAuthHeaders() }
  );
  if (!response.ok) throw new Error('Failed to get execution details');
  return response.json();
}

async terminateExecution(executionId: string, reason?: string): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/api/workflows/execution/${executionId}/terminate`,
    {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ reason })
    }
  );
  if (!response.ok) throw new Error('Failed to terminate execution');
}

async pauseExecution(executionId: string): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/api/workflows/execution/${executionId}/pause`,
    {
      method: 'POST',
      headers: getAuthHeaders()
    }
  );
  if (!response.ok) throw new Error('Failed to pause execution');
}

async resumeExecution(executionId: string): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/api/workflows/execution/${executionId}/resume`,
    {
      method: 'POST',
      headers: getAuthHeaders()
    }
  );
  if (!response.ok) throw new Error('Failed to resume execution');
}
```

### **Task 3: Add Real-Time WebSocket Subscriptions** (1 hour)
Extend `websocketService.ts`:
```typescript
// Add workflow-specific subscriptions
private workflowStatusCallbacks: Map<string, (status: any) => void> = new Map();

subscribeToWorkflowStatus(workflowId: string, callback: (status: any) => void) {
  if (!this.client || !this.connected) {
    console.warn('Not connected to WebSocket');
    return;
  }

  this.client.subscribe(`/topic/workflow/${workflowId}/status`, (message) => {
    const status = JSON.parse(message.body);
    callback(status);
  });

  this.workflowStatusCallbacks.set(workflowId, callback);
}

unsubscribeFromWorkflowStatus(workflowId: string) {
  this.workflowStatusCallbacks.delete(workflowId);
}
```

### **Task 4: Create Execution Monitor Components** (2 hours)

#### Component 1: `ExecutionStatusBadge.tsx`
```tsx
interface Props {
  status: ConductorWorkflowStatus;
}

export function ExecutionStatusBadge({ status }: Props) {
  const colors = {
    RUNNING: 'bg-blue-100 text-blue-800',
    COMPLETED: 'bg-green-100 text-green-800',
    FAILED: 'bg-red-100 text-red-800',
    TERMINATED: 'bg-gray-100 text-gray-800',
    PAUSED: 'bg-yellow-100 text-yellow-800',
    TIMED_OUT: 'bg-orange-100 text-orange-800',
  };
  
  return (
    <span className={`px-2 py-1 rounded text-xs font-medium ${colors[status]}`}>
      {status}
    </span>
  );
}
```

#### Component 2: `TaskExecutionTimeline.tsx`
```tsx
interface Props {
  tasks: TaskExecution[];
  workflowStatus: ConductorWorkflowStatus;
}

export function TaskExecutionTimeline({ tasks, workflowStatus }: Props) {
  return (
    <div className="space-y-2">
      {tasks.map((task) => (
        <div key={task.taskId} className="flex items-center gap-3 p-3 bg-white rounded border">
          <div className="flex-shrink-0">
            {task.status === 'COMPLETED' && <CheckCircleIcon className="h-6 w-6 text-green-500" />}
            {task.status === 'FAILED' && <XCircleIcon className="h-6 w-6 text-red-500" />}
            {task.status === 'IN_PROGRESS' && <ArrowPathIcon className="h-6 w-6 text-blue-500 animate-spin" />}
            {task.status === 'SCHEDULED' && <ClockIcon className="h-6 w-6 text-gray-400" />}
          </div>
          
          <div className="flex-1">
            <h4 className="font-medium">{task.referenceTaskName}</h4>
            <p className="text-sm text-gray-500">{task.taskType}</p>
          </div>
          
          <div className="text-right text-sm text-gray-500">
            {task.startTime && (
              <div>{new Date(task.startTime).toLocaleTimeString()}</div>
            )}
            {task.endTime && (
              <div>Duration: {((task.endTime - task.startTime) / 1000).toFixed(2)}s</div>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
```

#### Component 3: `ExecutionControls.tsx`
```tsx
interface Props {
  executionId: string;
  status: ConductorWorkflowStatus;
  onStatusChange: () => void;
}

export function ExecutionControls({ executionId, status, onStatusChange }: Props) {
  const handleTerminate = async () => {
    await workflowService.terminateExecution(executionId);
    onStatusChange();
  };

  const handlePause = async () => {
    await workflowService.pauseExecution(executionId);
    onStatusChange();
  };

  const handleResume = async () => {
    await workflowService.resumeExecution(executionId);
    onStatusChange();
  };

  return (
    <div className="flex gap-2">
      {status === 'RUNNING' && (
        <>
          <button onClick={handlePause} className="btn-secondary">
            <PauseIcon className="h-4 w-4" /> Pause
          </button>
          <button onClick={handleTerminate} className="btn-danger">
            <StopIcon className="h-4 w-4" /> Terminate
          </button>
        </>
      )}
      
      {status === 'PAUSED' && (
        <button onClick={handleResume} className="btn-primary">
          <PlayIcon className="h-4 w-4" /> Resume
        </button>
      )}
    </div>
  );
}
```

### **Task 5: Create Execution Details Page** (2 hours)
`app/workflows/executions/[executionId]/page.tsx`:
```tsx
'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import { workflowService } from '@/services/workflow.service';
import { websocketService } from '@/services/websocketService';
import { WorkflowExecution } from '@/types/conductor';
import { ExecutionStatusBadge } from '@/components/Workflow/ExecutionStatusBadge';
import { TaskExecutionTimeline } from '@/components/Workflow/TaskExecutionTimeline';
import { ExecutionControls } from '@/components/Workflow/ExecutionControls';

export default function ExecutionDetailsPage() {
  const params = useParams();
  const executionId = params.executionId as string;
  
  const [execution, setExecution] = useState<WorkflowExecution | null>(null);
  const [loading, setLoading] = useState(true);

  const loadExecution = async () => {
    try {
      const data = await workflowService.getExecutionDetails(executionId);
      setExecution(data);
    } catch (error) {
      console.error('Failed to load execution:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadExecution();

    // Subscribe to real-time updates
    websocketService.connect();
    websocketService.subscribeToWorkflowStatus(executionId, (status) => {
      setExecution((prev) => prev ? { ...prev, ...status } : null);
    });

    return () => {
      websocketService.unsubscribeFromWorkflowStatus(executionId);
    };
  }, [executionId]);

  if (loading) return <div>Loading...</div>;
  if (!execution) return <div>Execution not found</div>;

  return (
    <div className="max-w-7xl mx-auto p-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">{execution.workflowName}</h1>
          <p className="text-gray-500">Execution ID: {execution.workflowId}</p>
        </div>
        <ExecutionStatusBadge status={execution.status} />
      </div>

      <div className="mb-6">
        <ExecutionControls 
          executionId={executionId}
          status={execution.status}
          onStatusChange={loadExecution}
        />
      </div>

      <div className="grid grid-cols-3 gap-4 mb-8">
        <div className="bg-white p-4 rounded shadow">
          <div className="text-sm text-gray-500">Started</div>
          <div className="text-lg font-semibold">
            {new Date(execution.createTime).toLocaleString()}
          </div>
        </div>
        <div className="bg-white p-4 rounded shadow">
          <div className="text-sm text-gray-500">Duration</div>
          <div className="text-lg font-semibold">
            {execution.endTime 
              ? `${((execution.endTime - execution.createTime) / 1000).toFixed(2)}s`
              : 'Running...'}
          </div>
        </div>
        <div className="bg-white p-4 rounded shadow">
          <div className="text-sm text-gray-500">Tasks</div>
          <div className="text-lg font-semibold">
            {execution.tasks.filter(t => t.status === 'COMPLETED').length} / {execution.tasks.length}
          </div>
        </div>
      </div>

      <div className="bg-white rounded shadow p-6">
        <h2 className="text-xl font-bold mb-4">Task Execution Timeline</h2>
        <TaskExecutionTimeline tasks={execution.tasks} workflowStatus={execution.status} />
      </div>

      {execution.reasonForIncompletion && (
        <div className="mt-6 bg-red-50 border border-red-200 rounded p-4">
          <h3 className="font-semibold text-red-800">Error</h3>
          <p className="text-red-700">{execution.reasonForIncompletion}</p>
        </div>
      )}
    </div>
  );
}
```

### **Task 6: Update Workflow Detail Page** (1 hour)
Add "View Execution" links in `app/workflows/[id]/page.tsx`:
```tsx
// After executing workflow, redirect to execution details
const handleExecute = async () => {
  const result = await workflowService.executeWorkflow(id);
  router.push(`/workflows/executions/${result.workflowId}`);
};
```

## ⏱️ **Estimated Timeline**

| Task | Time | Priority |
|------|------|----------|
| 1. Update types | 30 min | High |
| 2. Enhance service | 45 min | High |
| 3. WebSocket subscriptions | 1 hour | High |
| 4. Monitor components | 2 hours | High |
| 5. Execution details page | 2 hours | High |
| 6. Update workflow page | 1 hour | Medium |
| **TOTAL** | **~7 hours** | |

## 🎯 **Success Criteria**

✅ User can execute a workflow and see real-time status updates  
✅ Task-by-task execution is visible with timestamps  
✅ User can pause/resume/terminate workflows  
✅ Errors are displayed with details  
✅ WebSocket updates happen without page refresh  
✅ All Conductor statuses are properly handled  

## 🚀 **Final Verdict**

**KEEP THE FRONTEND** - It's 70% complete. We just need to:
1. Add Conductor-specific types
2. Enhance service layer with execution APIs
3. Add real-time WebSocket monitoring
4. Create execution detail components
5. Wire everything together

The visual workflow editor, canvas, and basic structure are excellent and don't need replacement.

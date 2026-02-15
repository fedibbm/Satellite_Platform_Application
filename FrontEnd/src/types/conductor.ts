/**
 * Conductor Workflow Engine Type Definitions
 * Aligned with Netflix Conductor and backend API responses
 */

/**
 * Conductor workflow execution status
 */
export type ConductorWorkflowStatus = 
  | 'RUNNING' 
  | 'COMPLETED' 
  | 'FAILED' 
  | 'TERMINATED' 
  | 'PAUSED' 
  | 'TIMED_OUT';

/**
 * Task execution status within a workflow
 */
export type TaskStatus = 
  | 'SCHEDULED' 
  | 'IN_PROGRESS' 
  | 'COMPLETED' 
  | 'FAILED' 
  | 'FAILED_WITH_TERMINAL_ERROR'
  | 'TIMED_OUT' 
  | 'CANCELED' 
  | 'SKIPPED'
  | 'COMPLETED_WITH_ERRORS';

/**
 * Individual task execution details
 */
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
  taskDefName?: string;
  scheduledTime?: number;
  executed?: boolean;
  callbackFromWorker?: boolean;
  responseTimeoutSeconds?: number;
  workflowInstanceId?: string;
  workflowType?: string;
  taskDefinition?: any;
  rateLimitPerFrequency?: number;
  rateLimitFrequencyInSeconds?: number;
  externalInputPayloadStoragePath?: string;
  externalOutputPayloadStoragePath?: string;
  workflowPriority?: number;
  executionNameSpace?: string;
  isolationGroupId?: string;
  iteration?: number;
  subWorkflowId?: string;
  subworkflowChanged?: boolean;
  loopOverTask?: boolean;
}

/**
 * Complete workflow execution with all tasks
 */
export interface WorkflowExecution {
  workflowId: string;
  workflowName: string;
  workflowVersion?: number;
  status: ConductorWorkflowStatus;
  tasks: TaskExecution[];
  input: Record<string, any>;
  output: Record<string, any>;
  createTime: number;
  updateTime: number;
  endTime?: number;
  reasonForIncompletion?: string;
  failedReferenceTaskNames?: string[];
  correlationId?: string;
  reRunFromWorkflowId?: string;
  parentWorkflowId?: string;
  parentWorkflowTaskId?: string;
  event?: string;
  taskToDomain?: Record<string, string>;
  failedTaskNames?: string[];
  workflowDefinition?: any;
  externalInputPayloadStoragePath?: string;
  externalOutputPayloadStoragePath?: string;
  priority?: number;
  variables?: Record<string, any>;
  lastRetriedTime?: number;
}

/**
 * Execution history record from MongoDB
 */
export interface ExecutionHistory {
  id: string;
  workflowExecutionId: string;
  workflowDefinitionId: string;
  workflowName: string;
  projectId: string;
  executedBy: string;
  status: ExecutionHistoryStatus;
  startTime: string;
  endTime?: string;
  durationMs?: number;
  totalTasks: number;
  completedTasks: number;
  failedTasksCount: number;
  inputParameters: Record<string, any>;
  outputData?: Record<string, any>;
  failureReason?: string;
  failedTasks?: string[];
  lastUpdated: string;
}

/**
 * Execution history status (from MongoDB)
 */
export type ExecutionHistoryStatus =
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'TERMINATED'
  | 'PAUSED'
  | 'TIMED_OUT';

/**
 * Real-time workflow status update from WebSocket
 */
export interface WorkflowStatusUpdate {
  workflowId: string;
  status: ConductorWorkflowStatus;
  updateTime: number;
  completedTasks?: number;
  totalTasks?: number;
  output?: Record<string, any>;
  reasonForIncompletion?: string;
}

/**
 * Task status update from WebSocket
 */
export interface TaskStatusUpdate {
  workflowId: string;
  taskId: string;
  taskType: string;
  status: TaskStatus;
  updateTime: number;
  reasonForIncompletion?: string;
}

/**
 * Workflow execution request
 */
export interface ExecuteWorkflowRequest {
  parameters?: Record<string, any>;
}

/**
 * Workflow execution response
 */
export interface ExecuteWorkflowResponse {
  workflowId: string;
  conductorWorkflowName: string;
  status: string;
  message: string;
}

/**
 * Terminate workflow request
 */
export interface TerminateWorkflowRequest {
  reason?: string;
}

/**
 * Helper function to check if status is terminal (execution ended)
 */
export function isTerminalStatus(status: ConductorWorkflowStatus): boolean {
  return ['COMPLETED', 'FAILED', 'TERMINATED', 'TIMED_OUT'].includes(status);
}

/**
 * Helper function to check if status indicates success
 */
export function isSuccessStatus(status: ConductorWorkflowStatus): boolean {
  return status === 'COMPLETED';
}

/**
 * Helper function to check if status indicates failure
 */
export function isFailureStatus(status: ConductorWorkflowStatus): boolean {
  return ['FAILED', 'TIMED_OUT', 'TERMINATED'].includes(status);
}

/**
 * Helper function to get status color class
 */
export function getStatusColor(status: ConductorWorkflowStatus): string {
  switch (status) {
    case 'RUNNING':
      return 'bg-blue-100 text-blue-800 border-blue-200';
    case 'COMPLETED':
      return 'bg-green-100 text-green-800 border-green-200';
    case 'FAILED':
      return 'bg-red-100 text-red-800 border-red-200';
    case 'TERMINATED':
      return 'bg-gray-100 text-gray-800 border-gray-200';
    case 'PAUSED':
      return 'bg-yellow-100 text-yellow-800 border-yellow-200';
    case 'TIMED_OUT':
      return 'bg-orange-100 text-orange-800 border-orange-200';
    default:
      return 'bg-gray-100 text-gray-800 border-gray-200';
  }
}

/**
 * Helper function to get task status color class
 */
export function getTaskStatusColor(status: TaskStatus): string {
  switch (status) {
    case 'SCHEDULED':
      return 'text-gray-500';
    case 'IN_PROGRESS':
      return 'text-blue-500';
    case 'COMPLETED':
      return 'text-green-500';
    case 'FAILED':
    case 'FAILED_WITH_TERMINAL_ERROR':
      return 'text-red-500';
    case 'TIMED_OUT':
      return 'text-orange-500';
    case 'CANCELED':
      return 'text-gray-500';
    case 'SKIPPED':
      return 'text-gray-400';
    case 'COMPLETED_WITH_ERRORS':
      return 'text-yellow-500';
    default:
      return 'text-gray-500';
  }
}

/**
 * Calculate duration in human-readable format
 */
export function formatDuration(startTime: number, endTime?: number): string {
  const duration = (endTime || Date.now()) - startTime;
  const seconds = Math.floor(duration / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);

  if (hours > 0) {
    return `${hours}h ${minutes % 60}m`;
  } else if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`;
  } else {
    return `${seconds}s`;
  }
}

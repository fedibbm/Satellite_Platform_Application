import { Workflow, WorkflowExecution, CreateWorkflowData, UpdateWorkflowData } from '@/types/workflow';
import { 
  WorkflowExecution as ConductorWorkflowExecution,
  ExecuteWorkflowResponse,
  TerminateWorkflowRequest
} from '@/types/conductor';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9090';
const WORKFLOW_API = `${API_BASE_URL}/api/workflows`;

// Helper function to get auth headers
// Authentication is handled via HTTP-only cookies, not localStorage
const getAuthHeaders = () => {
  return {
    'Content-Type': 'application/json'
  };
};

// Helper function to get fetch options with credentials
const getFetchOptions = (method: string = 'GET', body?: any): RequestInit => {
  return {
    method,
    headers: getAuthHeaders(),
    credentials: 'include', // Include HTTP-only cookies for authentication
    ...(body && { body: JSON.stringify(body) })
  };
};

export const workflowService = {
  async getAllWorkflows(): Promise<Workflow[]> {
    const response = await fetch(WORKFLOW_API, getFetchOptions());
    if (!response.ok) throw new Error('Failed to fetch workflows');
    return response.json();
  },

  async getWorkflowById(id: string): Promise<Workflow> {
    const response = await fetch(`${WORKFLOW_API}/${id}`, getFetchOptions());
    if (!response.ok) throw new Error('Workflow not found');
    return response.json();
  },

  async createWorkflow(data: CreateWorkflowData): Promise<Workflow> {
    const response = await fetch(WORKFLOW_API, getFetchOptions('POST', data));
    if (!response.ok) throw new Error('Failed to create workflow');
    return response.json();
  },

  async updateWorkflow(id: string, data: UpdateWorkflowData): Promise<Workflow> {
    const response = await fetch(`${WORKFLOW_API}/${id}`, getFetchOptions('PUT', data));
    if (!response.ok) throw new Error('Failed to update workflow');
    return response.json();
  },

  async deleteWorkflow(id: string): Promise<void> {
    const response = await fetch(`${WORKFLOW_API}/${id}`, getFetchOptions('DELETE'));
    if (!response.ok) throw new Error('Failed to delete workflow');
  },

  async getWorkflowsByProject(projectId: string): Promise<Workflow[]> {
    const response = await fetch(`${WORKFLOW_API}/project/${projectId}`, getFetchOptions());
    if (!response.ok) throw new Error('Failed to fetch project workflows');
    return response.json();
  },

  async getWorkflowTemplates(): Promise<Workflow[]> {
    const response = await fetch(`${WORKFLOW_API}/templates`, getFetchOptions());
    if (!response.ok) throw new Error('Failed to fetch templates');
    return response.json();
  },

  async executeWorkflow(id: string, parameters?: Record<string, any>): Promise<{ message: string; workflowId: string }> {
    const response = await fetch(`${WORKFLOW_API}/${id}/execute`, 
      getFetchOptions('POST', { parameters: parameters || {} })
    );
    if (!response.ok) throw new Error('Failed to execute workflow');
    return response.json();
  },

  async getWorkflowExecutions(workflowId: string): Promise<WorkflowExecution[]> {
    const response = await fetch(`${WORKFLOW_API}/${workflowId}/executions`, getFetchOptions());
    if (!response.ok) throw new Error('Failed to fetch executions');
    return response.json();
  },

  async getExecutionById(executionId: string): Promise<WorkflowExecution> {
    const response = await fetch(`${WORKFLOW_API}/executions/${executionId}`, getFetchOptions());
    if (!response.ok) throw new Error('Execution not found');
    return response.json();
  },

  async getAllExecutions(): Promise<WorkflowExecution[]> {
    const response = await fetch(`${WORKFLOW_API}/executions`, getFetchOptions());
    if (!response.ok) throw new Error('Failed to fetch all executions');
    return response.json();
  },

  async cancelExecution(executionId: string): Promise<WorkflowExecution> {
    const response = await fetch(`${WORKFLOW_API}/executions/${executionId}/cancel`, 
      getFetchOptions('POST')
    );
    if (!response.ok) throw new Error('Failed to cancel execution');
    return response.json();
  },

  async getNodeTypes(): Promise<any[]> {
    const response = await fetch(`${WORKFLOW_API}/nodes/types`, getFetchOptions());
    if (!response.ok) throw new Error('Failed to fetch node types');
    return response.json();
  },

  // ============================================
  // Conductor Execution APIs
  // ============================================

  /**
   * Get real-time workflow execution status from Conductor
   */
  async getExecutionStatus(executionId: string): Promise<ConductorWorkflowExecution> {
    const response = await fetch(`${WORKFLOW_API}/execution/${executionId}/status`, getFetchOptions());
    if (!response.ok) throw new Error('Failed to get execution status');
    return response.json();
  },

  /**
   * Get detailed workflow execution with all task details from Conductor
   */
  async getExecutionDetails(executionId: string): Promise<ConductorWorkflowExecution> {
    const response = await fetch(`${WORKFLOW_API}/execution/${executionId}/details`, getFetchOptions());
    if (!response.ok) throw new Error('Failed to get execution details');
    return response.json();
  },

  /**
   * Terminate a running workflow execution
   */
  async terminateExecution(executionId: string, reason?: string): Promise<void> {
    const body: TerminateWorkflowRequest = reason ? { reason } : {};
    const response = await fetch(`${WORKFLOW_API}/execution/${executionId}/terminate`, 
      getFetchOptions('POST', body)
    );
    if (!response.ok) throw new Error('Failed to terminate execution');
  },

  /**
   * Pause a running workflow execution
   */
  async pauseExecution(executionId: string): Promise<void> {
    const response = await fetch(`${WORKFLOW_API}/execution/${executionId}/pause`, 
      getFetchOptions('POST')
    );
    if (!response.ok) throw new Error('Failed to pause execution');
  },

  /**
   * Resume a paused workflow execution
   */
  async resumeExecution(executionId: string): Promise<void> {
    const response = await fetch(`${WORKFLOW_API}/execution/${executionId}/resume`, 
      getFetchOptions('POST')
    );
    if (!response.ok) throw new Error('Failed to resume execution');
  },

  /**
   * Restart a completed workflow execution
   */
  async restartExecution(executionId: string): Promise<void> {
    const response = await fetch(`${WORKFLOW_API}/execution/${executionId}/restart`, 
      getFetchOptions('POST')
    );
    if (!response.ok) throw new Error('Failed to restart execution');
  },

  /**
   * Retry a failed workflow execution from the failed task
   */
  async retryExecution(executionId: string): Promise<void> {
    const response = await fetch(`${WORKFLOW_API}/execution/${executionId}/retry`, 
      getFetchOptions('POST')
    );
    if (!response.ok) throw new Error('Failed to retry execution');
  },

  /**
   * Register workflow with Conductor
   */
  async registerWorkflow(workflowId: string): Promise<{ message: string }> {
    const response = await fetch(`${WORKFLOW_API}/${workflowId}/register`, 
      getFetchOptions('POST')
    );
    if (!response.ok) throw new Error('Failed to register workflow');
    return response.json();
  }
};

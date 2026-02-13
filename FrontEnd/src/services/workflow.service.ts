import { Workflow, WorkflowExecution, CreateWorkflowData, UpdateWorkflowData } from '@/types/workflow';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
const WORKFLOW_API = `${API_BASE_URL}/api/workflows`;

// Helper function to get auth token
const getAuthHeaders = () => {
  const token = localStorage.getItem('access_token');
  return {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` })
  };
};

export const workflowService = {
  async getAllWorkflows(): Promise<Workflow[]> {
    const response = await fetch(WORKFLOW_API, {
      headers: getAuthHeaders()
    });
    if (!response.ok) throw new Error('Failed to fetch workflows');
    return response.json();
  },

  async getWorkflowById(id: string): Promise<Workflow> {
    const response = await fetch(`${WORKFLOW_API}/${id}`, {
      headers: getAuthHeaders()
    });
    if (!response.ok) throw new Error('Workflow not found');
    return response.json();
  },

  async createWorkflow(data: CreateWorkflowData): Promise<Workflow> {
    const response = await fetch(WORKFLOW_API, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(data)
    });
    if (!response.ok) throw new Error('Failed to create workflow');
    return response.json();
  },

  async updateWorkflow(id: string, data: UpdateWorkflowData): Promise<Workflow> {
    const response = await fetch(`${WORKFLOW_API}/${id}`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(data)
    });
    if (!response.ok) throw new Error('Failed to update workflow');
    return response.json();
  },

  async deleteWorkflow(id: string): Promise<void> {
    const response = await fetch(`${WORKFLOW_API}/${id}`, {
      method: 'DELETE',
      headers: getAuthHeaders()
    });
    if (!response.ok) throw new Error('Failed to delete workflow');
  },

  async getWorkflowsByProject(projectId: string): Promise<Workflow[]> {
    const response = await fetch(`${WORKFLOW_API}/project/${projectId}`, {
      headers: getAuthHeaders()
    });
    if (!response.ok) throw new Error('Failed to fetch project workflows');
    return response.json();
  },

  async getWorkflowTemplates(): Promise<Workflow[]> {
    const response = await fetch(`${WORKFLOW_API}/templates`, {
      headers: getAuthHeaders()
    });
    if (!response.ok) throw new Error('Failed to fetch templates');
    return response.json();
  },

  async executeWorkflow(id: string, parameters?: Record<string, any>): Promise<{ message: string; workflowId: string }> {
    const response = await fetch(`${WORKFLOW_API}/${id}/execute`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ parameters: parameters || {} })
    });
    if (!response.ok) throw new Error('Failed to execute workflow');
    return response.json();
  },

  async getWorkflowExecutions(workflowId: string): Promise<WorkflowExecution[]> {
    const response = await fetch(`${WORKFLOW_API}/${workflowId}/executions`, {
      headers: getAuthHeaders()
    });
    if (!response.ok) throw new Error('Failed to fetch executions');
    return response.json();
  },

  async getExecutionById(executionId: string): Promise<WorkflowExecution> {
    const response = await fetch(`${WORKFLOW_API}/executions/${executionId}`, {
      headers: getAuthHeaders()
    });
    if (!response.ok) throw new Error('Execution not found');
    return response.json();
  },

  async getAllExecutions(): Promise<WorkflowExecution[]> {
    const response = await fetch(`${WORKFLOW_API}/executions`, {
      headers: getAuthHeaders()
    });
    if (!response.ok) throw new Error('Failed to fetch all executions');
    return response.json();
  },

  async cancelExecution(executionId: string): Promise<WorkflowExecution> {
    const response = await fetch(`${WORKFLOW_API}/executions/${executionId}/cancel`, {
      method: 'POST',
      headers: getAuthHeaders()
    });
    if (!response.ok) throw new Error('Failed to cancel execution');
    return response.json();
  },

  async getNodeTypes(): Promise<any[]> {
    const response = await fetch(`${WORKFLOW_API}/nodes/types`, {
      headers: getAuthHeaders()
    });
    if (!response.ok) throw new Error('Failed to fetch node types');
    return response.json();
  }
};

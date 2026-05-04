import { Workflow, WorkflowExecution, WorkflowVersion, CreateWorkflowData, UpdateWorkflowData } from '@/types/workflow';
import { httpClient } from '@/utils/api/http-client';

export const workflowService = {
  async getAllWorkflows(): Promise<Workflow[]> {
    const response = await httpClient.get('/api/workflows');
    return response?.data || [];
  },

  async getWorkflowById(id: string): Promise<Workflow> {
    const response = await httpClient.get(`/api/workflows/${id}`);
    if (!response?.data) {
      throw new Error('Failed to fetch workflow');
    }
    return response.data;
  },

  async createWorkflow(data: CreateWorkflowData): Promise<Workflow> {
    const response = await httpClient.post('/api/workflows', data);
    console.log('Create workflow response:', response);
    console.log('Response data:', response?.data);
    
    if (!response?.data) {
      console.error('Invalid response structure:', response);
      throw new Error('Failed to create workflow. Please ensure the backend is running and restart it if needed.');
    }
    return response.data;
  },

  async updateWorkflow(id: string, data: UpdateWorkflowData): Promise<Workflow> {
    const response = await httpClient.put(`/api/workflows/${id}`, data);
    if (!response?.data) {
      throw new Error('Failed to update workflow');
    }
    return response.data;
  },

  async deleteWorkflow(id: string): Promise<void> {
    await httpClient.delete(`/api/workflows/${id}`);
  },

  async getWorkflowTemplates(): Promise<Workflow[]> {
    const response = await httpClient.get('/api/workflows/templates');
    return response?.data || [];
  },

  async executeWorkflow(id: string): Promise<WorkflowExecution> {
    const response = await httpClient.post(`/api/workflows/${id}/execute`, {});
    if (!response?.data) {
      throw new Error('Failed to execute workflow');
    }
    return response.data;
  },

  async getWorkflowExecutions(workflowId: string): Promise<WorkflowExecution[]> {
    const response = await httpClient.get(`/api/workflows/${workflowId}/executions`);
    return response?.data || [];
  },

  async getWorkflowsByProject(projectId: string): Promise<Workflow[]> {
    const response = await httpClient.get(`/api/workflows/project/${projectId}`);
    return response?.data || [];
  },

  async copyWorkflow(id: string, targetProjectId: string): Promise<Workflow> {
    const response = await httpClient.post(`/api/workflows/${id}/copy?targetProjectId=${targetProjectId}`, {});
    if (!response?.data) {
      throw new Error('Failed to copy workflow');
    }
    return response.data;
  },

  async getWorkflowVersions(id: string): Promise<WorkflowVersion[]> {
    const response = await httpClient.get(`/api/workflows/${id}/versions`);
    return response?.data || [];
  },

  async revertToVersion(id: string, version: string): Promise<Workflow> {
    const response = await httpClient.post(`/api/workflows/${id}/revert`, { version });
    if (!response?.data) {
      throw new Error('Failed to revert workflow');
    }
    return response.data;
  }
};

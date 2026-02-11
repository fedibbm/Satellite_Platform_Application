import { Workflow, WorkflowExecution, CreateWorkflowData, UpdateWorkflowData } from '@/types/workflow';
import { httpClient } from '@/utils/api/http-client';

export const workflowService = {
  async getAllWorkflows(): Promise<Workflow[]> {
    const response = await httpClient.get('/api/workflows');
    return response.data.data;
  },

  async getWorkflowById(id: string): Promise<Workflow> {
    const response = await httpClient.get(`/api/workflows/${id}`);
    return response.data.data;
  },

  async createWorkflow(data: CreateWorkflowData): Promise<Workflow> {
    const response = await httpClient.post('/api/workflows', data);
    return response.data.data;
  },

  async updateWorkflow(id: string, data: UpdateWorkflowData): Promise<Workflow> {
    const response = await httpClient.put(`/api/workflows/${id}`, data);
    return response.data.data;
  },

  async deleteWorkflow(id: string): Promise<void> {
    await httpClient.delete(`/api/workflows/${id}`);
  },

  async getWorkflowTemplates(): Promise<Workflow[]> {
    const response = await httpClient.get('/api/workflows/templates');
    return response.data.data;
  },

  async executeWorkflow(id: string): Promise<WorkflowExecution> {
    const response = await httpClient.post(`/api/workflows/${id}/execute`, {});
    return response.data.data;
  },

  async getWorkflowExecutions(workflowId: string): Promise<WorkflowExecution[]> {
    const response = await httpClient.get(`/api/workflows/${workflowId}/executions`);
    return response.data.data;
  }
};

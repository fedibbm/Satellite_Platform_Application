import { Workflow, WorkflowExecution, CreateWorkflowData, UpdateWorkflowData } from '@/types/workflow';

// Dummy data for now - will be replaced with actual API calls
const dummyWorkflows: Workflow[] = [
  {
    id: '1',
    name: 'Monthly NDVI Analysis',
    description: 'Automated monthly vegetation health monitoring using NDVI calculation',
    status: 'ACTIVE',
    projectId: 'proj-123',
    currentVersion: 'v1.2',
    createdAt: '2025-01-15T10:00:00Z',
    updatedAt: '2026-02-01T14:30:00Z',
    createdBy: 'user@example.com',
    tags: ['ndvi', 'vegetation', 'automated'],
    isTemplate: false,
    versions: [
      {
        version: 'v1.2',
        createdAt: '2026-02-01T14:30:00Z',
        createdBy: 'user@example.com',
        changelog: 'Added error handling for cloudy images',
        nodes: [
          {
            id: 'trigger-1',
            type: 'trigger',
            position: { x: 100, y: 100 },
            data: { label: 'Monthly Schedule', config: { cron: '0 0 1 * *' } }
          },
          {
            id: 'input-1',
            type: 'data-input',
            position: { x: 100, y: 200 },
            data: { label: 'GEE Sentinel-2 Import', config: { satellite: 'sentinel2' } }
          },
          {
            id: 'process-1',
            type: 'processing',
            position: { x: 100, y: 300 },
            data: { label: 'Calculate NDVI', config: { algorithm: 'ndvi' } }
          },
          {
            id: 'output-1',
            type: 'output',
            position: { x: 100, y: 400 },
            data: { label: 'Save Results', config: { format: 'geotiff' } }
          }
        ],
        edges: [
          { id: 'e1-2', source: 'trigger-1', target: 'input-1' },
          { id: 'e2-3', source: 'input-1', target: 'process-1' },
          { id: 'e3-4', source: 'process-1', target: 'output-1' }
        ]
      }
    ],
    executions: [
      {
        id: 'exec-1',
        workflowId: '1',
        version: 'v1.2',
        status: 'completed',
        startedAt: '2026-02-01T00:00:00Z',
        completedAt: '2026-02-01T00:15:30Z',
        triggeredBy: 'system',
        logs: [
          { timestamp: '2026-02-01T00:00:00Z', nodeId: 'trigger-1', level: 'info', message: 'Workflow started' },
          { timestamp: '2026-02-01T00:05:00Z', nodeId: 'input-1', level: 'info', message: 'Imported 12 images' },
          { timestamp: '2026-02-01T00:12:00Z', nodeId: 'process-1', level: 'info', message: 'NDVI calculation completed' },
          { timestamp: '2026-02-01T00:15:30Z', nodeId: 'output-1', level: 'info', message: 'Results saved successfully' }
        ]
      }
    ]
  },
  {
    id: '2',
    name: 'Change Detection Pipeline',
    description: 'Compare imagery from different dates to detect land use changes',
    status: 'DRAFT',
    currentVersion: 'v1.0',
    createdAt: '2026-02-05T09:00:00Z',
    updatedAt: '2026-02-08T16:45:00Z',
    createdBy: 'user@example.com',
    tags: ['change-detection', 'comparison'],
    isTemplate: false,
    versions: [
      {
        version: 'v1.0',
        createdAt: '2026-02-05T09:00:00Z',
        createdBy: 'user@example.com',
        nodes: [],
        edges: []
      }
    ],
    executions: []
  }
];

const workflowTemplates: Workflow[] = [
  {
    id: 'template-1',
    name: 'Basic Image Classification',
    description: 'Template for supervised image classification workflows',
    status: 'ACTIVE',
    currentVersion: 'v1.0',
    createdAt: '2025-12-01T10:00:00Z',
    updatedAt: '2025-12-01T10:00:00Z',
    createdBy: 'system',
    tags: ['classification', 'template'],
    isTemplate: true,
    versions: [
      {
        version: 'v1.0',
        createdAt: '2025-12-01T10:00:00Z',
        createdBy: 'system',
        nodes: [
          {
            id: 'trigger-1',
            type: 'trigger',
            position: { x: 100, y: 100 },
            data: { label: 'Manual Trigger' }
          },
          {
            id: 'input-1',
            type: 'data-input',
            position: { x: 100, y: 200 },
            data: { label: 'Load Training Data' }
          },
          {
            id: 'process-1',
            type: 'processing',
            position: { x: 100, y: 300 },
            data: { label: 'Train Classifier' }
          },
          {
            id: 'process-2',
            type: 'processing',
            position: { x: 100, y: 400 },
            data: { label: 'Apply Classification' }
          },
          {
            id: 'output-1',
            type: 'output',
            position: { x: 100, y: 500 },
            data: { label: 'Export Results' }
          }
        ],
        edges: [
          { id: 'e1-2', source: 'trigger-1', target: 'input-1' },
          { id: 'e2-3', source: 'input-1', target: 'process-1' },
          { id: 'e3-4', source: 'process-1', target: 'process-2' },
          { id: 'e4-5', source: 'process-2', target: 'output-1' }
        ]
      }
    ],
    executions: []
  }
];

export const workflowService = {
  async getAllWorkflows(token: string): Promise<Workflow[]> {
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 300));
    return dummyWorkflows;
  },

  async getWorkflowById(id: string, token: string): Promise<Workflow> {
    await new Promise(resolve => setTimeout(resolve, 200));
    const workflow = dummyWorkflows.find(w => w.id === id);
    if (!workflow) throw new Error('Workflow not found');
    return workflow;
  },

  async createWorkflow(data: CreateWorkflowData, token: string): Promise<Workflow> {
    await new Promise(resolve => setTimeout(resolve, 300));
    const newWorkflow: Workflow = {
      id: `wf-${Date.now()}`,
      name: data.name,
      description: data.description,
      status: 'DRAFT',
      projectId: data.projectId,
      currentVersion: 'v1.0',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      createdBy: 'current-user@example.com',
      tags: [],
      isTemplate: data.isTemplate || false,
      versions: [
        {
          version: 'v1.0',
          createdAt: new Date().toISOString(),
          createdBy: 'current-user@example.com',
          nodes: data.nodes || [],
          edges: data.edges || []
        }
      ],
      executions: []
    };
    dummyWorkflows.push(newWorkflow);
    return newWorkflow;
  },

  async updateWorkflow(id: string, data: UpdateWorkflowData, token: string): Promise<Workflow> {
    await new Promise(resolve => setTimeout(resolve, 300));
    const workflow = dummyWorkflows.find(w => w.id === id);
    if (!workflow) throw new Error('Workflow not found');
    
    Object.assign(workflow, data);
    workflow.updatedAt = new Date().toISOString();
    
    return workflow;
  },

  async deleteWorkflow(id: string, token: string): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 300));
    const index = dummyWorkflows.findIndex(w => w.id === id);
    if (index > -1) {
      dummyWorkflows.splice(index, 1);
    }
  },

  async getWorkflowTemplates(token: string): Promise<Workflow[]> {
    await new Promise(resolve => setTimeout(resolve, 200));
    return workflowTemplates;
  },

  async executeWorkflow(id: string, token: string): Promise<WorkflowExecution> {
    await new Promise(resolve => setTimeout(resolve, 500));
    const execution: WorkflowExecution = {
      id: `exec-${Date.now()}`,
      workflowId: id,
      version: 'v1.0',
      status: 'running',
      startedAt: new Date().toISOString(),
      triggeredBy: 'current-user@example.com',
      logs: [
        { timestamp: new Date().toISOString(), nodeId: 'trigger-1', level: 'info', message: 'Execution started' }
      ]
    };
    return execution;
  },

  async getWorkflowExecutions(workflowId: string, token: string): Promise<WorkflowExecution[]> {
    await new Promise(resolve => setTimeout(resolve, 200));
    const workflow = dummyWorkflows.find(w => w.id === workflowId);
    return workflow?.executions || [];
  }
};

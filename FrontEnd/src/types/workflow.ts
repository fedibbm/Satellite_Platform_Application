export type WorkflowStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'ARCHIVED';

export type NodeType = 
  | 'trigger'
  | 'data-input'
  | 'processing'
  | 'decision'
  | 'output';

export interface WorkflowNode {
  id: string;
  type: NodeType;
  position: { x: number; y: number };
  data: {
    label: string;
    description?: string;
    config?: Record<string, any>;
    status?: 'idle' | 'running' | 'success' | 'error';
  };
}

export interface WorkflowEdge {
  id: string;
  source: string;
  target: string;
  label?: string;
  type?: 'default' | 'conditional';
}

export interface WorkflowVersion {
  version: string;
  createdAt: string;
  createdBy: string;
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  changelog?: string;
}

export interface WorkflowExecution {
  id: string;
  workflowId: string;
  version: string;
  status: 'running' | 'completed' | 'failed' | 'cancelled';
  startedAt: string;
  completedAt?: string;
  triggeredBy: string;
  logs: WorkflowLog[];
  results?: Record<string, any>;
}

export interface WorkflowLog {
  timestamp: string;
  nodeId: string;
  level: 'info' | 'warning' | 'error';
  message: string;
}

export interface Workflow {
  id: string;
  name: string;
  description: string;
  status: WorkflowStatus;
  projectId?: string;
  currentVersion: string;
  versions: WorkflowVersion[];
  executions: WorkflowExecution[];
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  tags?: string[];
  isTemplate?: boolean;
}

export interface CreateWorkflowData {
  name: string;
  description: string;
  projectId?: string;
  isTemplate?: boolean;
  nodes?: WorkflowNode[];
  edges?: WorkflowEdge[];
}

export interface UpdateWorkflowData {
  name?: string;
  description?: string;
  status?: WorkflowStatus;
  nodes?: WorkflowNode[];
  edges?: WorkflowEdge[];
  changelog?: string;
}

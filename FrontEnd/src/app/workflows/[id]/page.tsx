'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { workflowService } from '@/services/workflow.service';
import { Workflow, WorkflowNode, WorkflowEdge } from '@/types/workflow';
import WorkflowCanvas from '@/components/Workflow/WorkflowCanvas';
import NodePalette from '@/components/Workflow/NodePalette';
import {
  ArrowLeftIcon,
  PlayIcon,
  ClockIcon,
  DocumentTextIcon,
  Cog6ToothIcon,
} from '@heroicons/react/24/outline';

export default function WorkflowDetailPage() {
  const router = useRouter();
  const params = useParams();
  const workflowId = params?.id as string;

  const [workflow, setWorkflow] = useState<Workflow | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'canvas' | 'versions' | 'executions' | 'settings'>('canvas');
  const [nodes, setNodes] = useState<WorkflowNode[]>([]);
  const [edges, setEdges] = useState<WorkflowEdge[]>([]);

  useEffect(() => {
    if (workflowId) {
      loadWorkflow();
    }
  }, [workflowId]);

  const loadWorkflow = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token') || '';
      const data = await workflowService.getWorkflowById(workflowId, token);
      setWorkflow(data);
      
      // Load current version nodes and edges
      const currentVersion = data.versions.find(v => v.version === data.currentVersion);
      if (currentVersion) {
        setNodes(currentVersion.nodes);
        setEdges(currentVersion.edges);
      }
    } catch (error) {
      console.error('Error loading workflow:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddNode = useCallback((type: string) => {
    const newNode: WorkflowNode = {
      id: `node-${Date.now()}`,
      type: type as any,
      position: { x: Math.random() * 400 + 100, y: Math.random() * 400 + 100 },
      data: {
        label: `New ${type} node`,
        description: '',
        config: {},
      },
    };
    setNodes((nds) => [...nds, newNode]);
  }, []);

  const handleNodesChange = useCallback((newNodes: WorkflowNode[]) => {
    setNodes(newNodes);
  }, []);

  const handleEdgesChange = useCallback((newEdges: WorkflowEdge[]) => {
    setEdges(newEdges);
  }, []);

  const handleSave = async () => {
    try {
      const token = localStorage.getItem('token') || '';
      await workflowService.updateWorkflow(
        workflowId,
        { nodes, edges, changelog: 'Updated workflow design' },
        token
      );
      alert('Workflow saved successfully!');
    } catch (error) {
      console.error('Error saving workflow:', error);
      alert('Failed to save workflow');
    }
  };

  const handleExecute = async () => {
    try {
      const token = localStorage.getItem('token') || '';
      await workflowService.executeWorkflow(workflowId, token);
      alert('Workflow execution started!');
      loadWorkflow(); // Reload to show new execution
    } catch (error) {
      console.error('Error executing workflow:', error);
      alert('Failed to execute workflow');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!workflow) {
    return (
      <div className="flex items-center justify-center h-screen">
        <p className="text-gray-500">Workflow not found</p>
      </div>
    );
  }

  return (
    <div className="h-screen flex flex-col bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button
              onClick={() => router.push('/workflows')}
              className="text-gray-600 hover:text-gray-900"
            >
              <ArrowLeftIcon className="h-5 w-5" />
            </button>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{workflow.name}</h1>
              <p className="text-sm text-gray-500">{workflow.description}</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-sm text-gray-600">v{workflow.currentVersion}</span>
            <button
              onClick={handleSave}
              className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
            >
              Save
            </button>
            <button
              onClick={handleExecute}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              <PlayIcon className="h-5 w-5" />
              Execute
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div className="mt-4 border-t border-gray-200 pt-4">
          <nav className="flex space-x-8">
            {[
              { id: 'canvas', label: 'Canvas', icon: Cog6ToothIcon },
              { id: 'versions', label: 'Versions', icon: ClockIcon },
              { id: 'executions', label: 'Executions', icon: PlayIcon },
              { id: 'settings', label: 'Settings', icon: DocumentTextIcon },
            ].map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as any)}
                  className={`${
                    activeTab === tab.id
                      ? 'text-blue-600 border-blue-600'
                      : 'text-gray-500 border-transparent hover:text-gray-700'
                  } flex items-center gap-2 border-b-2 pb-2 text-sm font-medium transition-colors`}
                >
                  <Icon className="h-4 w-4" />
                  {tab.label}
                </button>
              );
            })}
          </nav>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 flex overflow-hidden">
        {activeTab === 'canvas' && (
          <>
            <div className="w-64 bg-white border-r border-gray-200 p-4 overflow-y-auto">
              <NodePalette onAddNode={handleAddNode} />
            </div>
            <div className="flex-1">
              <WorkflowCanvas
                initialNodes={nodes}
                initialEdges={edges}
                onNodesChange={handleNodesChange}
                onEdgesChange={handleEdgesChange}
              />
            </div>
          </>
        )}

        {activeTab === 'versions' && (
          <div className="flex-1 p-6 overflow-y-auto">
            <h2 className="text-xl font-semibold mb-4">Version History</h2>
            <div className="space-y-4">
              {workflow.versions.map((version) => (
                <div
                  key={version.version}
                  className="bg-white rounded-lg p-4 border border-gray-200"
                >
                  <div className="flex items-center justify-between mb-2">
                    <span className="font-semibold">{version.version}</span>
                    {version.version === workflow.currentVersion && (
                      <span className="px-2 py-1 bg-blue-100 text-blue-700 text-xs rounded">
                        Current
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-gray-600">{version.changelog || 'No changelog'}</p>
                  <p className="text-xs text-gray-500 mt-2">
                    {new Date(version.createdAt).toLocaleString()} by {version.createdBy}
                  </p>
                </div>
              ))}
            </div>
          </div>
        )}

        {activeTab === 'executions' && (
          <div className="flex-1 p-6 overflow-y-auto">
            <h2 className="text-xl font-semibold mb-4">Execution History</h2>
            {workflow.executions.length > 0 ? (
              <div className="space-y-4">
                {workflow.executions.map((execution) => (
                  <div
                    key={execution.id}
                    className="bg-white rounded-lg p-4 border border-gray-200"
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-semibold">Execution {execution.id}</span>
                      <span
                        className={`px-2 py-1 text-xs rounded ${
                          execution.status === 'completed'
                            ? 'bg-green-100 text-green-700'
                            : execution.status === 'failed'
                            ? 'bg-red-100 text-red-700'
                            : execution.status === 'running'
                            ? 'bg-blue-100 text-blue-700'
                            : 'bg-gray-100 text-gray-700'
                        }`}
                      >
                        {execution.status}
                      </span>
                    </div>
                    <p className="text-sm text-gray-600">Version: {execution.version}</p>
                    <p className="text-xs text-gray-500 mt-2">
                      Started: {new Date(execution.startedAt).toLocaleString()}
                    </p>
                    {execution.completedAt && (
                      <p className="text-xs text-gray-500">
                        Completed: {new Date(execution.completedAt).toLocaleString()}
                      </p>
                    )}
                    {execution.logs.length > 0 && (
                      <div className="mt-3 space-y-1">
                        {execution.logs.slice(0, 3).map((log, idx) => (
                          <p key={idx} className="text-xs text-gray-600">
                            [{log.level}] {log.message}
                          </p>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-gray-500">No executions yet</p>
            )}
          </div>
        )}

        {activeTab === 'settings' && (
          <div className="flex-1 p-6 overflow-y-auto">
            <h2 className="text-xl font-semibold mb-4">Workflow Settings</h2>
            <div className="bg-white rounded-lg p-6 border border-gray-200 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
                <input
                  type="text"
                  value={workflow.name}
                  readOnly
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea
                  value={workflow.description}
                  readOnly
                  rows={3}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
                <select
                  value={workflow.status}
                  disabled
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50"
                >
                  <option value="DRAFT">Draft</option>
                  <option value="ACTIVE">Active</option>
                  <option value="PAUSED">Paused</option>
                  <option value="ARCHIVED">Archived</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Created</label>
                <p className="text-sm text-gray-600">
                  {new Date(workflow.createdAt).toLocaleString()} by {workflow.createdBy}
                </p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Last Updated</label>
                <p className="text-sm text-gray-600">
                  {new Date(workflow.updatedAt).toLocaleString()}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

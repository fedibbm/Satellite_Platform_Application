'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { workflowService } from '@/services/workflow.service';
import { Workflow, WorkflowNode, WorkflowEdge } from '@/types/workflow';
import { wsService } from '@/services/websocketService';
import WorkflowCanvas from '@/components/Workflow/WorkflowCanvas';
import NodePalette from '@/components/Workflow/NodePalette';
import NodeConfigPanel from '@/components/Workflow/NodeConfigPanel';
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
  const [selectedNode, setSelectedNode] = useState<WorkflowNode | null>(null);

  const subscriptionRef = useRef<any>(null);

  useEffect(() => {
    // Reconnect to ws if needed
    if (!wsService.isConnected) {
      wsService.connect();
    }
    
    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
      }
    };
  }, []);

  const subscribeToExecution = (executionId: string) => {
    if (subscriptionRef.current) {
       subscriptionRef.current.unsubscribe();
    }
    
    console.log(`[Workflow Debug] Subscribing to execution events for ID: ${executionId}`);
    // Slight delay to ensure connection is ready
    setTimeout(() => {
        subscriptionRef.current = wsService.subscribeToTopic(`/topic/workflow.execution.${executionId}`, (message) => {
           console.log(`[Workflow WebSocket Update] Status: ${message.status}`, message);
           
           if (message.status === 'NODE_START') {
               console.log(`➡️ [Executor] Node Started: ${message.data?.nodeId}`);
           } else if (message.status === 'NODE_COMPLETE') {
               console.log(`✅ [Executor] Node Completed: ${message.data?.nodeId}. Result payload attached.`);
           } else if (message.status === 'NODE_FAILED') {
               console.error(`❌ [Executor] Node Failed: ${message.data?.nodeId}`, message.error || message.data);
           } else if (message.status === 'FAILED') {
               console.error(`🚨 [Executor] Workflow Execution FAILED:`, message.error || message);
           } else if (message.status === 'COMPLETED') {
               console.log(`🎉 [Executor] Workflow Execution COMPLETED Successfully:`, message);
           }

           // Force reload of workflow to fetch updated execution status/nodes
           loadWorkflow(); 
           
           if (message.status === 'COMPLETED' || message.status === 'FAILED') {
               alert(`Workflow execution ${message.status}`);
           }
        });
    }, 1000);
  };


  useEffect(() => {
    if (workflowId) {
      loadWorkflow();
    }
  }, [workflowId]);

  const loadWorkflow = async () => {
    try {
      setLoading(true);
      const data = await workflowService.getWorkflowById(workflowId);
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

  const handleNodeClick = useCallback((nodeId: string) => {
    const node = nodes.find(n => n.id === nodeId);
    if (node) {
      setSelectedNode(node);
    }
  }, [nodes]);

  const handleNodeConfigSave = useCallback((nodeId: string, data: any) => {
    setNodes(prevNodes => 
      prevNodes.map(node => 
        node.id === nodeId
          ? { ...node, data: { ...node.data, ...data } }
          : node
      )
    );
  }, []);

  const handleSave = async () => {
    try {
      await workflowService.updateWorkflow(
        workflowId,
        { nodes, edges, changelog: 'Updated workflow design' }
      );
      alert('Workflow saved successfully!');
    } catch (error) {
      console.error('Error saving workflow:', error);
      alert('Failed to save workflow');
    }
  };

  const handleExecute = async () => {
    try {
      console.log(`[Workflow Debug] Triggering execution for Workflow ID: ${workflowId}...`);
      const execResp = await workflowService.executeWorkflow(workflowId);
      console.log(`[Workflow Debug] Execution Trigger Response API:`, execResp);
      
      alert('Workflow execution started! (Listening for real-time updates)');
      loadWorkflow(); // Reload to show new execution
      
      // Attempt to subscribe using returned execution ID if available
      if (execResp && execResp.data && execResp.data.id) {
          console.log(`[Workflow Debug] Hooking WebSocket onto execution payload ID: ${execResp.data.id}`);
          subscribeToExecution(execResp.data.id);
      } else if (execResp && execResp.id) {
          console.log(`[Workflow Debug] Hooking WebSocket onto execution payload ID: ${execResp.id}`);
          subscribeToExecution(execResp.id);
      } else {
          console.warn(`[Workflow Debug] Could not parse Execution ID from response to hook STOMP. Response was:`, execResp);
      }
    } catch (error) {
      console.error('Error executing workflow:', error);
      alert('Failed to execute workflow');
    }
  };

  const handleLogConfig = () => {
    const rawPayload = { workflow, nodes, edges };
    const scrubbedPayload = JSON.parse(JSON.stringify(rawPayload, (key, value) => {
      if (typeof value === 'string') {
        if (value.startsWith('data:image/') && value.length > 500) {
          return `[BASE64 IMAGE TRUNCATED - Size: ${(value.length / 1024).toFixed(2)} KB]`;
        }
        if (value.length > 10000) {
          return `[VERY LONG STRING TRUNCATED - Length: ${value.length} chars]`;
        }
      }
      return value;
    }));
    console.log('Workflow Configuration:', scrubbedPayload);
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
              onClick={handleLogConfig}
              className="px-4 py-2 bg-yellow-500 text-white rounded-lg hover:bg-yellow-600 transition-colors"
            >
              Log Config
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
                onNodeClick={handleNodeClick}
              />
            </div>
            
            {/* Node Config Panel */}
            <NodeConfigPanel
              node={selectedNode}
              onClose={() => setSelectedNode(null)}
              onSave={handleNodeConfigSave}
            />
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
                          execution.status === 'COMPLETED'
                            ? 'bg-green-100 text-green-700'
                            : execution.status === 'FAILED'
                            ? 'bg-red-100 text-red-700'
                            : execution.status === 'RUNNING'
                            ? 'bg-blue-100 text-blue-700'
                            : 'bg-gray-100 text-gray-700'
                        }`}
                      >
                        {execution.status.toLowerCase()}
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

'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { workflowService } from '@/services/workflow.service';
import { Workflow, WorkflowNode, WorkflowEdge } from '@/types/workflow';
import WorkflowCanvas from '@/components/Workflow/WorkflowCanvas';
import NodePalette from '@/components/Workflow/NodePalette';
import NodeConfigPanel from '@/components/Workflow/NodeConfigPanel';
import { wsService } from '@/services/websocketService';
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
  const [selectedExecutionId, setSelectedExecutionId] = useState<string | null>(null);

  useEffect(() => {
    if (workflowId) {
      loadWorkflow();
    }
  }, [workflowId]);

  // Synchronize via WebSockets if any execution is currently running
  useEffect(() => {
    let isSubscribed = true;
    const activeIds = workflow?.executions?.filter((e: any) => 
      ['RUNNING', 'PENDING', 'running', 'pending'].includes(e.status)
    ).map((e: any) => e.id) || [];

    if (activeIds.length === 0) return;

    const subscriptions: any[] = [];
    
    wsService.connect().then(() => {
      if (!isSubscribed) return;
      activeIds.forEach((id) => {
        const sub = wsService.subscribeToTopic(`/topic/workflow.execution.${id}`, (msg) => {
          console.log(`WebSocket update for execution ${id}:`, msg);
          loadWorkflow(false);
        });
        if (sub) subscriptions.push(sub);
      });
    }).catch(err => console.warn('WebSocket connection failed:', err));

    return () => {
      isSubscribed = false;
      subscriptions.forEach((sub) => {
        if (sub && typeof sub.unsubscribe === 'function') sub.unsubscribe();
      });
    };
  }, [workflow?.executions?.map((e: any) => `${e.id}-${e.status}`).join(',')]);

  const loadWorkflow = async (showLoading = true) => {
    try {
      if (showLoading) setLoading(true);
      const data = await workflowService.getWorkflowById(workflowId);
      setWorkflow((prev) => {
        // Avoid resetting nodes if we are just background polling
        if (showLoading || !prev) {
          const currentVersion = data.versions.find((v: any) => v.version === data.currentVersion);
          if (currentVersion) {
            setNodes(currentVersion.nodes);
            setEdges(currentVersion.edges);
          }
        }
        return data;
      });
    } catch (error) {
      console.error('Error loading workflow:', error);
    } finally {
      if (showLoading) setLoading(false);
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
      loadWorkflow(false); // Reload to show the newly saved version
    } catch (error) {
      console.error('Error saving workflow:', error);
      alert('Failed to save workflow');
    }
  };

  const handleExecute = async () => {
    try {
      await workflowService.executeWorkflow(workflowId);
      // Wait a moment then load to trigger polling loop
      setTimeout(() => loadWorkflow(false), 500); 
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
                  onClick={() => {
                    setActiveTab(tab.id as any);
                    setSelectedExecutionId(null);
                  }}
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
              {workflow.versions
                .sort((a, b) => {
                  const parse = (v: string) => {
                    const nums = v.replace(/^v/, '').split('.').map(Number);
                    return nums;
                  };
                  const aParts = parse(a.version);
                  const bParts = parse(b.version);
                  for (let i = 0; i < Math.max(aParts.length, bParts.length); i++) {
                    const diff = (bParts[i] || 0) - (aParts[i] || 0);
                    if (diff !== 0) return diff;
                  }
                  return 0;
                })
                .map((version) => (
                  <div
                    key={version.version}
                    className="bg-white rounded-lg p-4 border border-gray-200 flex items-start justify-between"
                  >
                    <div>
                      <div className="flex items-center gap-2 mb-2">
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
                    {version.version !== workflow.currentVersion && (
                      <button
                        onClick={async () => {
                          if (!confirm(`Revert to ${version.version}? The current version will be preserved but will no longer be active.`)) return;
                          try {
                            await workflowService.revertToVersion(workflowId, version.version);
                            await loadWorkflow(false);
                          } catch (error: any) {
                            alert(error.message || 'Failed to revert version');
                          }
                        }}
                        className="shrink-0 px-3 py-1.5 text-sm border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
                      >
                        Revert to this version
                      </button>
                    )}
                  </div>
                ))}
            </div>
          </div>
        )}

        {activeTab === 'executions' && (
          <div className="flex-1 p-6 overflow-y-auto">
            {selectedExecutionId ? (() => {
              const execution = workflow.executions.find(e => e.id === selectedExecutionId);
              if (!execution) return <p className="text-gray-500">Execution not found</p>;
              
              return (
                <div className="space-y-6">
                  <div className="flex items-center gap-4">
                    <button
                      onClick={() => setSelectedExecutionId(null)}
                      className="text-gray-600 hover:text-gray-900 flex items-center gap-2 font-medium"
                    >
                      <ArrowLeftIcon className="h-4 w-4" /> Back to list
                    </button>
                    <h2 className="text-xl font-semibold m-0 flex-1">Execution {execution.id}</h2>
                    <span
                      className={`px-3 py-1 text-sm font-semibold rounded ${
                        execution.status === 'COMPLETED'
                          ? 'bg-green-100 text-green-800'
                          : execution.status === 'FAILED'
                          ? 'bg-red-100 text-red-800'
                          : execution.status === 'RUNNING'
                          ? 'bg-blue-100 text-blue-800'
                          : 'bg-gray-100 text-gray-800'
                      }`}
                    >
                      {execution.status}
                    </span>
                  </div>

                  <div className="bg-white rounded-lg p-5 border border-gray-200">
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-sm font-medium text-gray-500">Version</p>
                        <p className="font-semibold text-gray-800">{execution.version}</p>
                      </div>
                      <div>
                        <p className="text-sm font-medium text-gray-500">Trigger Node</p>
                        <p className="font-semibold text-gray-800">{(execution as any).triggerNodeId || 'N/A'}</p>
                      </div>
                      <div>
                        <p className="text-sm font-medium text-gray-500">Started At</p>
                        <p className="font-semibold text-gray-800">{new Date(execution.startedAt).toLocaleString()}</p>
                      </div>
                      <div>
                        <p className="text-sm font-medium text-gray-500">Completed At</p>
                        <p className="font-semibold text-gray-800">{execution.completedAt ? new Date(execution.completedAt).toLocaleString() : 'In Progress...'}</p>
                      </div>
                    </div>
                  </div>

                  <div className="bg-gray-900 rounded-lg p-4 font-mono text-sm text-gray-100 h-96 overflow-y-auto">
                    <h3 className="text-gray-400 font-semibold mb-3 tracking-wider uppercase">Execution Logs</h3>
                    {execution.logs.length > 0 ? (
                      <div className="space-y-2 break-all">
                        {execution.logs.map((log, idx) => (
                          <div key={idx} className="flex gap-4">
                            <span className="text-gray-500 whitespace-nowrap">{new Date(log.timestamp).toLocaleTimeString()}</span>
                            <span
                              className={`w-16 font-bold whitespace-nowrap ${
                                log.level === 'ERROR' ? 'text-red-400' :
                                log.level === 'WARNING' ? 'text-yellow-400' :
                                'text-blue-400'
                              }`}
                            >
                              [{log.level}]
                            </span>
                            <span className={log.level === 'ERROR' ? 'text-red-300' : 'text-gray-200'}>{log.message}</span>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p className="text-gray-500 italic">No logs available for this execution.</p>
                    )}
                  </div>
                  
                  {/* Results Section */}
                  {execution.status === 'COMPLETED' && execution.results && Object.keys(execution.results).length > 0 && (
                    <div className="mt-6">
                      <h3 className="text-lg font-semibold mb-3 text-gray-800">Execution Results (By Node)</h3>
                      <div className="space-y-4">
                        {Object.entries(execution.results).map(([nodeId, resultData]: [string, any]) => {
                          const nodeLabel = nodes.find(n => n.id === nodeId)?.data?.label || nodeId;
                          const hasImage = resultData?.processedImageBase64 || resultData?.imageUrl || resultData?.imageId;
                          
                          return (
                            <div key={nodeId} className="bg-white rounded-lg p-4 border border-gray-200 shadow-sm">
                              <h4 className="font-semibold text-blue-800 mb-2 border-b pb-2">{nodeLabel}</h4>
                              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div className="bg-gray-50 p-3 rounded overflow-x-auto text-sm font-mono text-gray-700">
                                  <pre>{JSON.stringify(
                                    Object.fromEntries(Object.entries(resultData).filter(([k]) => k !== 'processedImageBase64')), 
                                    null, 2
                                  )}</pre>
                                </div>
                                {hasImage && (
                                  <div className="flex flex-col items-center justify-center p-4 bg-gray-100 rounded border border-gray-200">
                                    <span className="text-xs text-gray-500 mb-3 uppercase font-semibold">Image Asset</span>
                                    {resultData.processedImageBase64 ? (
                                      <img 
                                        src={`data:image/png;base64,${resultData.processedImageBase64}`} 
                                        alt={`Result from ${nodeLabel}`} 
                                        className="max-h-64 object-contain rounded shadow-sm"
                                      />
                                    ) : resultData.imageUrl ? (
                                      <img 
                                        src={resultData.imageUrl} 
                                        alt={`Result from ${nodeLabel}`} 
                                        className="max-h-64 object-contain rounded shadow-sm"
                                      />
                                    ) : resultData.imageId ? (
                                      <div className="text-center flex flex-col items-center">
                                         <p className="text-sm text-gray-600 mb-3">
                                           {resultData.filename || 'Raw Output (.tif)'}
                                           <br/><span className="text-xs text-gray-500">(Browsers supported by backend proxy preview)</span>
                                         </p>
                                         <a
                                           href={`${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/geospatial/images/${resultData.imageId}/data`}
                                           target="_blank"
                                           rel="noreferrer"
                                           className="block relative group cursor-pointer border rounded shadow-sm hover:shadow-md transition overflow-hidden"
                                           title="Click to Download Full GeoTIFF File"
                                         >
                                            <img
                                              src={`${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/geospatial/images/${resultData.imageId}/data`}
                                              alt={`Result from ${nodeLabel}`}
                                              className="max-h-64 object-contain"
                                              onError={(e) => {
                                                const target = e.currentTarget as HTMLImageElement;
                                                target.onerror = null; // prevent loop
                                                target.style.display = 'none'; // Hide broken image since it is a raw format
                                              }}
                                            />
                                            <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-20 flex items-center justify-center transition-all">
                                              <span className="opacity-0 group-hover:opacity-100 bg-black text-white px-3 py-1 rounded text-sm font-semibold">
                                                Download GeoTIFF
                                              </span>
                                            </div>
                                         </a>
                                      </div>
                                    ) : null}
                                  </div>
                                )}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </div>
              );
            })() : (
              // Execution List View
              <>
                <h2 className="text-xl font-semibold mb-4">Execution History</h2>
                {workflow.executions.length > 0 ? (
                  <div className="space-y-4">
                    {workflow.executions.map((execution) => (
                      <div
                        key={execution.id}
                        onClick={() => setSelectedExecutionId(execution.id)}
                        className="bg-white rounded-lg p-4 border border-gray-200 cursor-pointer hover:border-blue-400 hover:shadow-sm transition-all"
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
                              <p key={idx} className="text-xs text-gray-600 truncate">
                                <span className={log.level === 'ERROR' ? 'text-red-500 font-bold' : ''}>[{log.level}]</span> {log.message}
                              </p>
                            ))}
                            {execution.logs.length > 3 && (
                              <p className="text-xs text-blue-500 italic mt-1">View {execution.logs.length - 3} more logs...</p>
                            )}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-gray-500">No executions yet</p>
                )}
              </>
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

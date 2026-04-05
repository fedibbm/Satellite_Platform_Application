'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { workflowService } from '@/services/workflow.service';
import { Workflow, WorkflowNode, WorkflowEdge } from '@/types/workflow';
import { wsService } from '@/services/websocketService';
import { getAllProjects, getProject } from '@/services/projects.service';
import { Project } from '@/types/api';
import Modal from '@/components/Modal';
import { Snackbar, Alert } from '@mui/material';
import WorkflowCanvas from '@/components/Workflow/WorkflowCanvas';
import NodePalette from '@/components/Workflow/NodePalette';
import NodeConfigPanel from '@/components/Workflow/NodeConfigPanel';
import {
  ArrowLeftIcon,
  PlayIcon,
  ClockIcon,
  DocumentTextIcon,
  Cog6ToothIcon,
  DocumentDuplicateIcon,
} from '@heroicons/react/24/outline';

export default function WorkflowDetailPage() {
  const router = useRouter();
  const params = useParams();
  const workflowId = params?.id as string;

  const [workflow, setWorkflow] = useState<Workflow | null>(null);
  const [selectedExecution, setSelectedExecution] = useState<any | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'canvas' | 'versions' | 'executions' | 'settings'>('canvas');
  const [toast, setToast] = useState<{ open: boolean; message: string; severity: 'success' | 'error' | 'info' | 'warning' }>({ open: false, message: '', severity: 'info' });
  const handleCloseToast = () => setToast({ ...toast, open: false });
  const [nodes, setNodes] = useState<WorkflowNode[]>([]);
  const [edges, setEdges] = useState<WorkflowEdge[]>([]);
    const [selectedNode, setSelectedNode] = useState<WorkflowNode | null>(null);
  const [isCopyModalOpen, setIsCopyModalOpen] = useState(false);
  const [projects, setProjects] = useState<Project[]>([]);
  const [copyTargetProjectId, setCopyTargetProjectId] = useState<string>('');
  const [copying, setCopying] = useState(false);
  const [activeProjectName, setActiveProjectName] = useState<string>('Loading...');

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
               setToast({ open: true, message: `Workflow execution ${message.status}`, severity: 'info' });
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
      
      if (data.projectId) {
        getProject(data.projectId)
          .then(p => setActiveProjectName(p.projectName || p.name || 'Unknown Project'))
          .catch(() => setActiveProjectName('Unknown/Deleted Project'));
      } else {
        setActiveProjectName('Unassigned');
      }

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
      setToast({ open: true, message: 'Workflow saved successfully!', severity: 'success' });
    } catch (error) {
      console.error('Error saving workflow:', error);
      setToast({ open: true, message: 'Failed to save workflow', severity: 'error' });
    }
  };


  const handleOpenCopyModal = async () => {
    setIsCopyModalOpen(true);
    try {
      const response = await getAllProjects(0, 100);
      setProjects(response.content || []);
      if (response.content?.length > 0) {
        if (response.content[0].id) setCopyTargetProjectId(response.content[0].id);
      }
    } catch (e) {
      console.error('Failed to load projects for copy');
    }
  };

  const handleCopyWorkflow = async () => {
    if (!copyTargetProjectId) return;
    setCopying(true);
    try {
      const copied = await workflowService.copyWorkflow(workflowId, copyTargetProjectId);
      setToast({ open: true, message: 'Workflow copied successfully!', severity: 'success' });
      setIsCopyModalOpen(false);
      router.push(`/workflows/${copied.id}`);
    } catch (error) {
      console.error(error);
      setToast({ open: true, message: 'Failed to copy workflow', severity: 'error' });
    } finally {
      setCopying(false);
    }
  };

  const handleExecute = async () => {
    try {
      console.log(`[Workflow Debug] Triggering execution for Workflow ID: ${workflowId}...`);
      const execResp = await workflowService.executeWorkflow(workflowId);
      console.log(`[Workflow Debug] Execution Trigger Response API:`, execResp);
      
      setToast({ open: true, message: 'Workflow execution started! (Listening for real-time updates)', severity: 'success' });
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
      setToast({ open: true, message: 'Failed to execute workflow', severity: 'error' });
    }
  };

  const handleLogConfig = () => {
    // Clone workflow to avoid mutating state and remove bulky arrays before logging
    const workflowCopy = { ...workflow };
    delete workflowCopy.versions;
    delete workflowCopy.executions;

    const rawPayload = { workflow: workflowCopy, nodes, edges };
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
              <div className="flex items-center gap-3">
                <h1 className="text-2xl font-bold text-gray-900">{workflow.name}</h1>
                <span className="inline-flex items-center rounded-md bg-blue-50 px-2 py-1 text-xs font-medium text-blue-700 ring-1 ring-inset ring-blue-700/10">
                  Project: {activeProjectName}
                </span>
              </div>
              <p className="text-sm text-gray-500 mt-1">{workflow.description}</p>
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
              onClick={handleOpenCopyModal}
              className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
            >
              <DocumentDuplicateIcon className="h-5 w-5" />
              Duplicate
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
                    onClick={() => setSelectedExecution(execution)}
                    className="bg-white rounded-lg p-4 border border-gray-200 cursor-pointer hover:shadow-md hover:border-blue-400 transition-all group relative"
                  >
                    <div className="absolute top-4 right-4 opacity-0 group-hover:opacity-100 text-xs text-blue-600 font-medium transition-opacity">
                      View Results & Logs &rarr;
                    </div>
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

      
      <Modal
        open={isCopyModalOpen}
        onClose={() => !copying && setIsCopyModalOpen(false)}
        title="Duplicate Workflow"
        actions={[
          { label: 'Cancel', onClick: () => setIsCopyModalOpen(false), disabled: copying, color: 'inherit' },
          { label: copying ? 'Copying...' : 'Copy', onClick: handleCopyWorkflow, disabled: copying || !copyTargetProjectId, color: 'primary', variant: 'contained' }
        ]}
        content={(
          <div className="p-4">
            <p className="mb-4 text-sm text-gray-600">Select a project to copy this workflow to.</p>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Target Project *
            </label>
            <select
              value={copyTargetProjectId}
              onChange={(e) => setCopyTargetProjectId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md"
              disabled={copying || projects.length === 0}
            >
              <option value="" disabled>Select a project</option>
              {projects.map(p => (
                <option key={p.id || p._id} value={p.id || p._id}>{p.projectName || p.name}</option>
              ))}
            </select>
          </div>
        )}
      />

      {/* Execution Details Modal */}
      <Modal
        open={!!selectedExecution}
        onClose={() => setSelectedExecution(null)}
        title={`Execution Details: ${selectedExecution?.id}`}
        maxWidth="lg"
        content={
          selectedExecution ? (
            <div className="space-y-6 text-sm py-2">
              <div className="grid grid-cols-2 gap-4 bg-gray-50 p-4 rounded-lg border border-gray-200">
                <div>
                  <span className="font-semibold block text-gray-700">Status</span>
                  <span className={`inline-flex px-2 py-1 mt-1 text-xs rounded font-medium ${
                    selectedExecution.status === 'COMPLETED' ? 'bg-green-100 text-green-700' :
                    selectedExecution.status === 'FAILED' ? 'bg-red-100 text-red-700' :
                    selectedExecution.status === 'RUNNING' ? 'bg-blue-100 text-blue-700' : 'bg-gray-200 text-gray-700'
                  }`}>
                    {selectedExecution.status}
                  </span>
                </div>
                <div>
                  <span className="font-semibold block text-gray-700">Time Execution</span>
                  <div className="text-gray-600 mt-1">
                    <div>Started: {new Date(selectedExecution.startedAt).toLocaleString()}</div>
                    {selectedExecution.completedAt && <div>Finished: {new Date(selectedExecution.completedAt).toLocaleString()}</div>}
                  </div>
                </div>
              </div>

              <div>
                <h3 className="text-lg font-semibold border-b pb-2 mb-3 text-gray-800">Results / Outputs</h3>
                {selectedExecution.results && Object.keys(selectedExecution.results).length > 0 ? (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {Object.entries(selectedExecution.results).map(([nodeId, result]: [string, any]) => {
                      
                      // Aggressive extraction of image URLs or Base64 data from backend responses
                      let displayImage = null;
                      
                      // Case 1: Processing Node emits full base64 string
                      if (result?.processedImageBase64 && !result.processedImageBase64.includes('truncated')) {
                         displayImage = result.processedImageBase64.startsWith('data:image') 
                           ? result.processedImageBase64 
                           : `data:image/png;base64,${result.processedImageBase64}`;
                      } 
                      // Case 2: Direct URL properties
                      else if (typeof result?.imageUrl === 'string') { displayImage = result.imageUrl; }
                      else if (typeof result?.downloadUrl === 'string') { displayImage = result.downloadUrl; }
                      else if (typeof result?.url === 'string') { displayImage = result.url; }
                      // Case 3: GEE Node wraps data in response.data object
                      else if (result?.data && typeof result.data === 'object' && !Array.isArray(result.data)) {
                         if (typeof result.data.url === 'string') displayImage = result.data.url;
                         else if (typeof result.data.downloadUrl === 'string') displayImage = result.data.downloadUrl;
                      }
                      // Case 4: GEE Node internally downloaded and saved Image object
                      else if (typeof result?.imageId === 'string') {
                          // Construct dynamic download URL directly from backend via auth boundaries
                          displayImage = `${process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'}/geospatial/images/${result.imageId}/data`;
                      }
                      
                      return (
                      <div key={nodeId} className="border border-gray-200 rounded-lg overflow-hidden flex flex-col bg-white hover:shadow-md transition-shadow">
                        <div className="bg-gray-50 px-3 py-2 border-b border-gray-200 text-xs font-semibold text-gray-700 flex justify-between items-center">
                          <span>Node: {nodeId}</span>
                        </div>
                        
                        <div className="p-3 flex-1 flex flex-col items-center justify-center bg-gray-50">
                          {displayImage ? (
                            <img 
                              src={displayImage} 
                              alt={`Output from ${nodeId}`}
                              className="max-h-48 object-contain rounded drop-shadow-sm mb-2"
                              onError={(e) => {
                                // Fallback if image fails to load
                                (e.target as HTMLImageElement).style.display = 'none';
                                (e.target as HTMLImageElement).nextElementSibling?.classList.remove('hidden');
                              }}
                            />
                          ) : null}
                          
                          <div className={`text-xs text-gray-500 text-center w-full ${displayImage ? 'hidden' : ''}`}>
                            <div className="bg-gray-900 text-green-400 p-3 rounded text-left overflow-x-auto w-full font-mono mt-0 max-h-48 overflow-y-auto">
                              <pre>{JSON.stringify(result, null, 2)}</pre>
                            </div>
                          </div>
                        </div>

                        {result?.statistics && typeof result.statistics === 'object' && !Array.isArray(result.statistics) && (
                          <div className="px-3 pb-3 bg-gray-50">
                            <h4 className="text-xs font-bold text-gray-600 mb-2 uppercase tracking-wider text-center">Statistics</h4>
                            <div className="grid grid-cols-2 lg:grid-cols-3 gap-2 text-xs">
                              {Object.entries(result.statistics).map(([key, val]: [string, any]) => (
                                <div key={key} className="bg-white p-2 border border-gray-200 rounded justify-between flex flex-col md:flex-row items-center gap-1">
                                  <span className="text-gray-500 capitalize">{key}</span>
                                  <span className="font-mono font-medium text-gray-800">
                                    {typeof val === 'number' ? Number(val.toFixed(4)) : String(val)}
                                  </span>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                        
                         {displayImage && (
                          <div className="p-2 border-t border-gray-100 bg-white flex justify-center">
                            <a 
                              href={displayImage} 
                              target="_blank" 
                              rel="noopener noreferrer"
                              download={`result_${nodeId}.png`}
                              className="text-xs text-blue-600 hover:text-blue-800 hover:underline px-2 py-1 flex items-center gap-1"
                            >
                              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                              View / Download 
                            </a>
                          </div>
                        )}
                      </div>
                    )})}
                  </div>
                ) : (
                  <div className="text-gray-500 italic p-6 bg-gray-50 rounded-lg border border-dashed border-gray-300 text-center">
                    No results produced by this execution yet.
                  </div>
                )}
              </div>

              <div>
                <h3 className="text-lg font-semibold border-b pb-2 mb-3 text-gray-800">Execution Logs</h3>
                {selectedExecution.logs && selectedExecution.logs.length > 0 ? (
                  <div className="bg-gray-900 rounded-lg p-4 overflow-y-auto max-h-96 font-mono text-xs space-y-1 shadow-inner">
                    {selectedExecution.logs.map((log: any, idx: number) => (
                      <div key={idx} className={`flex gap-3 ${
                        log.level === 'ERROR' ? 'text-red-400' :
                        log.level === 'WARNING' ? 'text-yellow-400' : 'text-gray-300'
                      }`}>
                        <span className="text-gray-500 whitespace-nowrap">[{new Date(log.timestamp).toLocaleTimeString()}]</span> 
                        <span className="font-bold whitespace-nowrap">[{log.level}]</span> 
                        <span className="break-all whitespace-pre-wrap">{log.message}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-gray-500 italic p-6 bg-gray-50 rounded-lg border border-dashed border-gray-300 text-center">No logs recorded.</div>
                )}
              </div>
            </div>
          ) : <></>
        }
        actions={[
          { label: 'Close', onClick: () => setSelectedExecution(null), color: 'inherit', variant: 'outlined' }
        ]}
      />

    </div>
  );
}

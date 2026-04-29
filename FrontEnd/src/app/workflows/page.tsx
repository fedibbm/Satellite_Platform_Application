'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { workflowService } from '@/services/workflow.service';
import { Workflow, WorkflowStatus } from '@/types/workflow';
import { Snackbar, Alert, CircularProgress } from '@mui/material';
import {
  PlusIcon,
  PlayIcon,
  PauseIcon,
  ClockIcon,
  RocketLaunchIcon,
} from '@heroicons/react/24/outline';

const statusColors = {
  DRAFT: 'bg-gray-100 text-gray-800',
  ACTIVE: 'bg-green-100 text-green-800',
  PAUSED: 'bg-yellow-100 text-yellow-800',
  ARCHIVED: 'bg-blue-100 text-blue-800',
} as const;

import { getAllProjects } from '@/services/projects.service';

export default function WorkflowsPage() {
  const router = useRouter();
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [templates, setTemplates] = useState<Workflow[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'workflows' | 'templates'>('workflows');
  const [toast, setToast] = useState<{ open: boolean; message: string; severity: 'success' | 'error' | 'info' | 'warning' }>({ open: false, message: '', severity: 'info' });
  const [executingId, setExecutingId] = useState<string | null>(null);

  const handleCloseToast = () => setToast({ ...toast, open: false });

  useEffect(() => {
    loadData();
  }, []);

    const loadData = async () => {
    try {
      setLoading(true);
      const [ownedWorkflowsData, templatesData] = await Promise.all([
        workflowService.getAllWorkflows(),
        workflowService.getWorkflowTemplates(),
      ]);
      
      let sharedWorkflows = [];
      try {
        const projectsResponse = await getAllProjects(0, 100);
        if (projectsResponse?.content && projectsResponse.content.length > 0) {
          const projectPromises = projectsResponse.content.map(p => {
             const pId = p.id || (p as any)._id;
             if (!pId) return Promise.resolve([]);
             return workflowService.getWorkflowsByProject(pId).catch(() => []);
          });
          const results = await Promise.all(projectPromises);
          sharedWorkflows = results.flat();
        }
      } catch (err) {
        console.warn("Could not fetch project workflows", err);
      }
      
      const allWfs = [...(ownedWorkflowsData || []), ...sharedWorkflows];
      const uniqueWfs = Array.from(new Map(allWfs.map(item => [item.id, item])).values());

      setWorkflows(uniqueWfs);
      setTemplates(templatesData || []);
    } catch (error) {
      console.error('Error loading workflows:', error);
      setWorkflows([]);
      setTemplates([]);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateWorkflow = () => {
    router.push('/workflows/new');
  };

  const handleWorkflowClick = (id: string) => {
    router.push(`/workflows/${id}`);
  };

  const handleExecuteWorkflow = async (e: React.MouseEvent, id: string) => {
    e.stopPropagation();
    
    // Pre-Flight Validation
    const workflow = workflows.find(w => w.id === id);
    if (workflow) {
      const currentVer = workflow.versions?.find(v => v.version === workflow.currentVersion);
      if (currentVer && (currentVer.nodes?.length === 0 || !currentVer.nodes)) {
        setToast({ open: true, message: 'Cannot execute: Workflow has no nodes connected.', severity: 'error' });
        return;
      }
    }

    try {
      setExecutingId(id);
      await workflowService.executeWorkflow(id);
      setToast({ open: true, message: 'Workflow execution started successfully!', severity: 'success' });
    } catch (error) {
      console.error('Error executing workflow:', error);
      setToast({ open: true, message: 'Failed to execute workflow.', severity: 'error' });
    } finally {
      setExecutingId(null);
    }
  };

  const WorkflowCard = ({ workflow }: { workflow: Workflow }) => {
    const lastExecution = workflow.executions?.[0];
    
    return (
      <div
        onClick={() => handleWorkflowClick(workflow.id)}
        className="cursor-pointer bg-white rounded-lg shadow-sm p-6 hover:shadow-md transition-shadow border border-gray-200"
      >
        <div className="flex items-start justify-between mb-3">
          <div className="flex-1">
            <div className="flex items-center gap-2">
              <h3 className="text-lg font-semibold text-gray-900">{workflow.name}</h3>
              {workflow.isTemplate && (
                <span className="inline-flex items-center rounded-full bg-purple-100 px-2 py-0.5 text-xs font-medium text-purple-700">
                  Template
                </span>
              )}
            </div>
            <p className="mt-1 text-sm text-gray-500 line-clamp-2">
              {workflow.description}
            </p>
          </div>
          <span
            className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
              statusColors[workflow.status as WorkflowStatus]
            }`}
          >
            {workflow.status}
          </span>
        </div>

        <div className="flex items-center justify-between text-xs text-gray-500">
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-1">
              <ClockIcon className="h-4 w-4" />
              <span>v{workflow.currentVersion}</span>
            </div>
            {lastExecution && (
              <div className="flex items-center gap-1">
                <RocketLaunchIcon className="h-4 w-4" />
                <span>Last: {new Date(lastExecution.startedAt).toLocaleDateString()}</span>
              </div>
            )}
          </div>
          
          {workflow.status === 'ACTIVE' && !workflow.isTemplate && (
            <button
              onClick={(e) => handleExecuteWorkflow(e, workflow.id)}
              disabled={executingId === workflow.id}
              className={`flex items-center gap-1 px-3 py-1 text-white rounded transition-colors ${executingId === workflow.id ? 'bg-blue-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700'}`}
            >
              {executingId === workflow.id ? (
                <CircularProgress size={16} color="inherit" />
              ) : (
                <PlayIcon className="h-4 w-4" />
              )}
              <span>{executingId === workflow.id ? 'Executing...' : 'Execute'}</span>
            </button>
          )}
        </div>

        {workflow.tags && workflow.tags.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-1">
            {workflow.tags.map((tag, index) => (
              <span
                key={index}
                className="inline-flex items-center rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700"
              >
                {tag}
              </span>
            ))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">Workflows</h1>
              <p className="mt-2 text-sm text-gray-600">
                Create and manage automated processing pipelines with versioning and reproducibility
              </p>
            </div>
            <button
              onClick={handleCreateWorkflow}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              <PlusIcon className="h-5 w-5" />
              New Workflow
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div className="border-b border-gray-200 mb-6">
          <nav className="-mb-px flex space-x-8">
            <button
              onClick={() => setActiveTab('workflows')}
              className={`${
                activeTab === 'workflows'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
              } whitespace-nowrap border-b-2 py-4 px-1 text-sm font-medium transition-colors`}
            >
              My Workflows ({workflows.length})
            </button>
            <button
              onClick={() => setActiveTab('templates')}
              className={`${
                activeTab === 'templates'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
              } whitespace-nowrap border-b-2 py-4 px-1 text-sm font-medium transition-colors`}
            >
              Templates ({templates.length})
            </button>
          </nav>
        </div>

        {/* Content */}
        {loading ? (
          <div className="flex items-center justify-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {activeTab === 'workflows' ? (
              workflows.length > 0 ? (
                workflows.map((workflow) => (
                  <WorkflowCard key={workflow.id} workflow={workflow} />
                ))
              ) : (
                <div className="col-span-full text-center py-16 bg-white rounded-xl border border-dashed border-gray-300">
                  <div className="mx-auto h-12 w-12 text-gray-400 mb-4 bg-gray-50 rounded-full flex items-center justify-center">
                    <RocketLaunchIcon className="h-6 w-6" />
                  </div>
                  <h3 className="text-lg font-medium text-gray-900 mb-1">No workflows yet</h3>
                  <p className="text-gray-500 mb-4">Create your first automated processing pipeline to get started.</p>
                  <button onClick={handleCreateWorkflow} className="inline-flex flex-col items-center justify-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
                    Create Workflow
                  </button>
                </div>
              )
            ) : (
              templates.length > 0 ? (
                templates.map((template) => (
                  <WorkflowCard key={template.id} workflow={template} />
                ))
              ) : (
                <div className="col-span-full text-center py-12">
                  <p className="text-gray-500">No templates available</p>
                </div>
              )
            )}
          </div>
        )}
      </div>
      <Snackbar open={toast.open} autoHideDuration={6000} onClose={handleCloseToast} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert onClose={handleCloseToast} severity={toast.severity} sx={{ width: '100%' }}>
          {toast.message}
        </Alert>
      </Snackbar>
    </div>
  );
}

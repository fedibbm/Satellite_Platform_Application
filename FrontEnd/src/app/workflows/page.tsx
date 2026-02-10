'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { workflowService } from '@/services/workflow.service';
import { Workflow, WorkflowStatus } from '@/types/workflow';
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

export default function WorkflowsPage() {
  const router = useRouter();
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [templates, setTemplates] = useState<Workflow[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'workflows' | 'templates'>('workflows');

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token') || '';
      const [workflowsData, templatesData] = await Promise.all([
        workflowService.getAllWorkflows(token),
        workflowService.getWorkflowTemplates(token),
      ]);
      setWorkflows(workflowsData);
      setTemplates(templatesData);
    } catch (error) {
      console.error('Error loading workflows:', error);
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
    try {
      const token = localStorage.getItem('token') || '';
      await workflowService.executeWorkflow(id, token);
      alert('Workflow execution started!');
    } catch (error) {
      console.error('Error executing workflow:', error);
      alert('Failed to execute workflow');
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
              className="flex items-center gap-1 px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors"
            >
              <PlayIcon className="h-4 w-4" />
              <span>Execute</span>
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
                <div className="col-span-full text-center py-12">
                  <p className="text-gray-500">No workflows yet. Create your first workflow to get started!</p>
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
    </div>
  );
}

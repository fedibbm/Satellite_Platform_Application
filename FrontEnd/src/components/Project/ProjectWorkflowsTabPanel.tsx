import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { workflowService } from '@/services/workflow.service';
import { Workflow } from '@/types/workflow';
import {
  RocketLaunchIcon,
  PlayIcon,
  PauseIcon,
  PlusIcon
} from '@heroicons/react/24/outline';
import { CircularProgress } from '@mui/material';

interface ProjectWorkflowsTabPanelProps {
  projectId: string;
}

const statusColors = {
  DRAFT: 'bg-gray-100 text-gray-800',
  ACTIVE: 'bg-green-100 text-green-800',
  PAUSED: 'bg-yellow-100 text-yellow-800',
  ARCHIVED: 'bg-blue-100 text-blue-800',
} as const;

export default function ProjectWorkflowsTabPanel({ projectId }: ProjectWorkflowsTabPanelProps) {
  const router = useRouter();
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadWorkflows();
  }, [projectId]);

  const loadWorkflows = async () => {
    try {
      setLoading(true);
      const workflowsData = await workflowService.getWorkflowsByProject(projectId);
      setWorkflows(workflowsData || []);
    } catch (error) {
      console.error('Error loading project workflows:', error);
      setWorkflows([]);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateWorkflow = () => {
    // If your app allows creating a workflow pre-linked to a project, pass ?projectId=${projectId}
    // but the default flow might not auto-link until the user configures it.
    router.push('/workflows/new');
  };

  const handleWorkflowClick = (id: string) => {
    router.push(`/workflows/${id}`);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center p-12">
        <CircularProgress size={40} />
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      <div className="flex justify-between items-center bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
        <div>
          <h2 className="text-xl font-bold text-gray-800">Project Workflows</h2>
          <p className="text-sm text-gray-500">Automated processing pipelines for this project</p>
        </div>
        <button
          onClick={handleCreateWorkflow}
          className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
        >
          <PlusIcon className="w-5 h-5" />
          <span className="font-semibold">New Workflow</span>
        </button>
      </div>

      {workflows.length === 0 ? (
        <div className="bg-white rounded-xl border border-dashed border-gray-300 p-12 text-center text-gray-500">
          <RocketLaunchIcon className="w-12 h-12 mx-auto text-gray-300 mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">No Workflows Available</h3>
          <p className="max-w-md mx-auto">Create and link a workflow here to process Earth Engine queries automatically and build pipelines inside your project environment.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {workflows.map((workflow) => (
            <div
              key={workflow.id}
              onClick={() => handleWorkflowClick(workflow.id)}
              className="group bg-white rounded-xl border border-gray-200 p-6 hover:shadow-md hover:border-blue-300 transition cursor-pointer flex flex-col"
            >
              <div className="flex justify-between items-start mb-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-blue-50 rounded-lg flex items-center justify-center text-blue-600 group-hover:scale-110 transition">
                    <RocketLaunchIcon className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="font-bold text-gray-900 truncate pr-2" title={workflow.name}>{workflow.name}</h3>
                  </div>
                </div>
              </div>

              <p className="text-gray-600 text-sm line-clamp-2 mb-4 flex-grow">
                {workflow.description || 'No description provided'}
              </p>

              <div className="flex items-center justify-between mt-auto pt-4 border-t border-gray-100">
                <span className={`px-2.5 py-1 text-xs font-semibold rounded-full border border-black/5 ${statusColors[workflow.status as keyof typeof statusColors] || statusColors.DRAFT}`}>
                  {workflow.status}
                </span>
                
                <span className="text-xs text-gray-500 font-medium bg-gray-50 px-2 py-1 rounded">
                  v{workflow.currentVersion}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

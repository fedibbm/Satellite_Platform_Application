'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { workflowService } from '@/services/workflow.service';
import { getAllProjects } from '@/services/projects.service';
import { Project } from '@/types/api';
import { ArrowLeftIcon } from '@heroicons/react/24/outline';

export default function NewWorkflowPage() {
  const router = useRouter();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [projectId, setProjectId] = useState<string>('');
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(false);
  const [fetchingProjects, setFetchingProjects] = useState(true);

  useEffect(() => {
    const fetchProjects = async () => {
      try {
        const response = await getAllProjects(0, 100);
        setProjects(response.content || []);
        if (response.content && response.content.length > 0) {
          const firstProject = response.content[0];
          setProjectId(firstProject.id || firstProject._id || '');
        }
      } catch (error) {
        console.error('Error fetching projects:', error);
      } finally {
        setFetchingProjects(false);
      }
    };
    fetchProjects();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!name.trim()) {
      alert('Please enter a workflow name');
      return;
    }
    
    if (!projectId) {
      alert('Please select a project for this workflow');
      return;
    }

    try {
      setLoading(true);
      const workflow = await workflowService.createWorkflow(
        {
          name,
          description,
          projectId,
          nodes: [],
          edges: [],
        }
      );
      
      if (!workflow || !workflow.id) {
        throw new Error('Invalid response from server. Please restart the backend.');
      }
      
      router.push(`/workflows/${workflow.id}`);
    } catch (error) {
      console.error('Error creating workflow:', error);
      const errorMessage = error instanceof Error ? error.message : 'Failed to create workflow';
      alert(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-8">
          <button
            onClick={() => router.push('/workflows')}
            className="flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-4"
          >
            <ArrowLeftIcon className="h-5 w-5" />
            Back to Workflows
          </button>
          <h1 className="text-3xl font-bold text-gray-900">Create New Workflow</h1>
          <p className="mt-2 text-sm text-gray-600">
            Set up a new automated processing pipeline inside a project
          </p>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
                Workflow Name *
              </label>
              <input
                type="text"
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="e.g., Monthly NDVI Analysis"
                required
              />
            </div>
            
            <div>
              <label htmlFor="projectId" className="block text-sm font-medium text-gray-700 mb-1">
                Project *
              </label>
              <select
                id="projectId"
                value={projectId}
                onChange={(e) => setProjectId(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                disabled={fetchingProjects}
                required
              >
                <option value="" disabled>Select a Project</option>
                {projects.map((project) => (
                  <option key={project.id || project._id} value={project.id || project._id}>
                    {project.projectName || project.name}
                  </option>
                ))}
              </select>
              {fetchingProjects && <p className="mt-1 text-sm text-gray-500">Loading projects...</p>}
            </div>

            <div>
              <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={4}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Describe what this workflow does..."
              />
            </div>

            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => router.push('/workflows')}
                className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading || !projectId}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
              >
                {loading ? 'Creating...' : 'Create Workflow'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

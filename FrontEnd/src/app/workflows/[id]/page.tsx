'use client';

import { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { workflowService } from '@/services/workflow.service';
import { Workflow } from '@/types/workflow';
import {
  ArrowLeftIcon,
  PlayIcon,
  DocumentTextIcon,
  CheckCircleIcon,
  XCircleIcon,
} from '@heroicons/react/24/outline';

export default function WorkflowDetailPage() {
  const router = useRouter();
  const params = useParams();
  const workflowId = params?.id as string;

  const [workflow, setWorkflow] = useState<Workflow | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'details' | 'executions'>('details');
  const [registering, setRegistering] = useState(false);
  const [isRegistered, setIsRegistered] = useState(false);

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
      setIsRegistered(data.nodes && data.nodes.length > 0);
    } catch (error) {
      console.error('Error loading workflow:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async () => {
    try {
      setRegistering(true);
      await workflowService.registerWorkflow(workflowId);
      alert('Workflow registered with Conductor successfully!');
      setIsRegistered(true);
    } catch (error) {
      console.error('Error registering workflow:', error);
      alert('Failed to register workflow');
    } finally {
      setRegistering(false);
    }
  };

  const handleExecute = async () => {
    if (!isRegistered) {
      alert('Please register the workflow with Conductor first');
      return;
    }
    try {
      const result = await workflowService.executeWorkflow(workflowId);
      router.push(`/workflows/executions/${result.workflowId}`);
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
            <span className="text-sm text-gray-600">v{workflow.version || '1.0'}</span>
            {isRegistered ? (
              <span className="flex items-center gap-1 text-sm text-green-600">
                <CheckCircleIcon className="h-4 w-4" />
                Registered
              </span>
            ) : (
              <span className="flex items-center gap-1 text-sm text-gray-500">
                <XCircleIcon className="h-4 w-4" />
                Not Registered
              </span>
            )}
            {!isRegistered && (
              <button
                onClick={handleRegister}
                disabled={registering}
                className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors disabled:opacity-50"
              >
                {registering ? 'Registering...' : 'Register with Conductor'}
              </button>
            )}
            <button
              onClick={handleExecute}
              disabled={!isRegistered}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
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
              { id: 'details', label: 'Details', icon: DocumentTextIcon },
              { id: 'executions', label: 'Executions', icon: PlayIcon },
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
      <div className="flex-1 overflow-hidden">
        {activeTab === 'details' && (
          <div className="h-full p-6 overflow-y-auto">
            <div className="max-w-4xl mx-auto space-y-6">
              <div className="bg-white rounded-lg p-6 border border-gray-200">
                <h2 className="text-xl font-semibold mb-4">Workflow Information</h2>
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
                    <p className="text-sm text-gray-900">{workflow.name}</p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                    <p className="text-sm text-gray-900">{workflow.description}</p>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
                      <span className={`inline-flex px-2 py-1 text-xs rounded ${
                        workflow.status === 'ACTIVE' ? 'bg-green-100 text-green-700' :
                        workflow.status === 'DRAFT' ? 'bg-gray-100 text-gray-700' :
                        'bg-yellow-100 text-yellow-700'
                      }`}>
                        {workflow.status}
                      </span>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Version</label>
                      <p className="text-sm text-gray-900">{workflow.version || '1.0'}</p>
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Created</label>
                      <p className="text-sm text-gray-600">
                        {new Date(workflow.createdAt).toLocaleString()}
                      </p>
                      <p className="text-xs text-gray-500">by {workflow.createdBy}</p>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Last Updated</label>
                      <p className="text-sm text-gray-600">
                        {new Date(workflow.updatedAt).toLocaleString()}
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg p-6 border border-gray-200">
                <h2 className="text-xl font-semibold mb-4">Workflow Tasks</h2>
                {workflow.nodes && workflow.nodes.length > 0 ? (
                  <div className="space-y-3">
                    {workflow.nodes.map((node, index) => (
                      <div key={node.id} className="border border-gray-200 rounded p-4">
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <div className="flex items-center gap-2">
                              <span className="text-sm font-medium text-gray-500">#{index + 1}</span>
                              <h3 className="font-semibold">{node.data?.label || node.id}</h3>
                              <span className="px-2 py-0.5 text-xs bg-blue-100 text-blue-700 rounded">
                                {node.type}
                              </span>
                            </div>
                            {node.data?.description && (
                              <p className="text-sm text-gray-600 mt-1">{node.data.description}</p>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-8">
                    <p className="text-gray-500">No tasks configured yet</p>
                    <button
                      onClick={() => router.push(`/workflows/${workflowId}/edit`)}
                      className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                    >
                      Configure Tasks
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {activeTab === 'executions' && (
          <div className="h-full p-6 overflow-y-auto">
            <div className="max-w-4xl mx-auto">
              <div className="bg-white rounded-lg p-6 border border-gray-200 text-center">
                <p className="text-gray-500 mb-4">
                  To view execution details, execute this workflow and monitor it in real-time
                </p>
                <button
                  onClick={handleExecute}
                  disabled={!isRegistered}
                  className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
                >
                  <PlayIcon className="h-5 w-5" />
                  Execute Workflow
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

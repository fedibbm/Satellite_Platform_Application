'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { workflowService } from '@/services/workflow.service';
import { wsService } from '@/services/websocketService';
import { 
  WorkflowExecution, 
  TaskExecution,
  formatDuration,
  isTerminalStatus 
} from '@/types/conductor';
import ExecutionStatusBadge from '@/components/Workflow/ExecutionStatusBadge';
import TaskExecutionTimeline from '@/components/Workflow/TaskExecutionTimeline';
import ExecutionControls from '@/components/Workflow/ExecutionControls';
import {
  ArrowLeftIcon,
  ClockIcon,
  PlayIcon,
  CheckCircleIcon,
  XCircleIcon
} from '@heroicons/react/24/outline';

export default function ExecutionDetailsPage() {
  const params = useParams();
  const router = useRouter();
  const executionId = params.executionId as string;
  
  const [execution, setExecution] = useState<WorkflowExecution | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedTask, setSelectedTask] = useState<TaskExecution | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(true);

  const loadExecution = async () => {
    try {
      setError(null);
      const data = await workflowService.getExecutionDetails(executionId);
      setExecution(data);
      
      // Disable auto-refresh if execution is terminal
      if (data && isTerminalStatus(data.status)) {
        setAutoRefresh(false);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load execution');
      console.error('Failed to load execution:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadExecution();

    // Connect WebSocket for real-time updates
    wsService.connect();
    
    // Subscribe to workflow status updates
    const unsubscribe = wsService.subscribeToWorkflowStatus(executionId, (statusUpdate) => {
      console.log('Received workflow status update:', statusUpdate);
      setExecution((prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          status: statusUpdate.status,
          updateTime: statusUpdate.updateTime,
          output: statusUpdate.output || prev.output,
          reasonForIncompletion: statusUpdate.reasonForIncompletion || prev.reasonForIncompletion
        };
      });
      
      // Stop auto-refresh when terminal
      if (isTerminalStatus(statusUpdate.status)) {
        setAutoRefresh(false);
      }
    });

    return () => {
      unsubscribe();
      wsService.unsubscribeFromWorkflowStatus(executionId);
    };
  }, [executionId]);

  // Auto-refresh for non-terminal executions
  useEffect(() => {
    if (!autoRefresh || !execution || isTerminalStatus(execution.status)) {
      return;
    }

    const interval = setInterval(() => {
      loadExecution();
    }, 5000); // Refresh every 5 seconds

    return () => clearInterval(interval);
  }, [autoRefresh, execution]);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading execution details...</p>
        </div>
      </div>
    );
  }

  if (error || !execution) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center max-w-md">
          <XCircleIcon className="h-16 w-16 text-red-500 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Failed to Load Execution</h2>
          <p className="text-gray-600 mb-6">{error || 'Execution not found'}</p>
          <button
            onClick={() => router.push('/workflows')}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            Back to Workflows
          </button>
        </div>
      </div>
    );
  }

  const completedTasks = execution.tasks.filter(t => t.status === 'COMPLETED').length;
  const failedTasks = execution.tasks.filter(t => 
    ['FAILED', 'FAILED_WITH_TERMINAL_ERROR', 'TIMED_OUT'].includes(t.status)
  ).length;
  const duration = formatDuration(execution.createTime, execution.endTime);

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-6">
          <button
            onClick={() => router.back()}
            className="inline-flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-4"
          >
            <ArrowLeftIcon className="h-5 w-5" />
            <span>Back</span>
          </button>

          <div className="flex items-start justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900 mb-2">
                {execution.workflowName}
              </h1>
              <p className="text-gray-500 font-mono text-sm">
                Execution ID: {execution.workflowId}
              </p>
              {execution.correlationId && (
                <p className="text-gray-500 text-sm mt-1">
                  Correlation ID: {execution.correlationId}
                </p>
              )}
            </div>
            <ExecutionStatusBadge status={execution.status} size="lg" showIcon />
          </div>
        </div>

        {/* Controls */}
        <div className="mb-6">
          <ExecutionControls 
            executionId={executionId}
            status={execution.status}
            onStatusChange={loadExecution}
          />
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
          <div className="bg-white p-6 rounded-lg shadow-sm border">
            <div className="flex items-center gap-3">
              <PlayIcon className="h-8 w-8 text-blue-600" />
              <div>
                <div className="text-sm text-gray-500">Started</div>
                <div className="text-lg font-semibold text-gray-900">
                  {new Date(execution.createTime).toLocaleString()}
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-lg shadow-sm border">
            <div className="flex items-center gap-3">
              <ClockIcon className="h-8 w-8 text-purple-600" />
              <div>
                <div className="text-sm text-gray-500">Duration</div>
                <div className="text-lg font-semibold text-gray-900">
                  {duration}
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-lg shadow-sm border">
            <div className="flex items-center gap-3">
              <CheckCircleIcon className="h-8 w-8 text-green-600" />
              <div>
                <div className="text-sm text-gray-500">Completed Tasks</div>
                <div className="text-lg font-semibold text-gray-900">
                  {completedTasks} / {execution.tasks.length}
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-lg shadow-sm border">
            <div className="flex items-center gap-3">
              <XCircleIcon className="h-8 w-8 text-red-600" />
              <div>
                <div className="text-sm text-gray-500">Failed Tasks</div>
                <div className="text-lg font-semibold text-gray-900">
                  {failedTasks}
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Error Banner */}
        {execution.reasonForIncompletion && (
          <div className="mb-6 bg-red-50 border-l-4 border-red-500 p-4 rounded">
            <div className="flex">
              <div className="flex-shrink-0">
                <XCircleIcon className="h-5 w-5 text-red-500" />
              </div>
              <div className="ml-3">
                <h3 className="text-sm font-medium text-red-800">
                  Workflow Failed
                </h3>
                <div className="mt-2 text-sm text-red-700">
                  <p>{execution.reasonForIncompletion}</p>
                  {execution.failedReferenceTaskNames && execution.failedReferenceTaskNames.length > 0 && (
                    <p className="mt-2">
                      Failed tasks: {execution.failedReferenceTaskNames.join(', ')}
                    </p>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Task Timeline */}
        <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-bold text-gray-900">Task Execution Timeline</h2>
            {!isTerminalStatus(execution.status) && (
              <div className="flex items-center gap-2">
                <div className="animate-pulse flex items-center gap-2 text-sm text-blue-600">
                  <div className="h-2 w-2 bg-blue-600 rounded-full"></div>
                  <span>Live</span>
                </div>
              </div>
            )}
          </div>
          <TaskExecutionTimeline 
            tasks={execution.tasks} 
            onTaskClick={setSelectedTask}
          />
        </div>

        {/* Input/Output */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Input Parameters */}
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Input Parameters</h3>
            <pre className="bg-gray-50 p-4 rounded text-sm overflow-auto max-h-96">
              {JSON.stringify(execution.input, null, 2)}
            </pre>
          </div>

          {/* Output */}
          {execution.output && Object.keys(execution.output).length > 0 && (
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Output</h3>
              <pre className="bg-gray-50 p-4 rounded text-sm overflow-auto max-h-96">
                {JSON.stringify(execution.output, null, 2)}
              </pre>
            </div>
          )}
        </div>

        {/* Task Details Modal */}
        {selectedTask && (
          <div 
            className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
            onClick={() => setSelectedTask(null)}
          >
            <div 
              className="bg-white rounded-lg max-w-4xl w-full max-h-[90vh] overflow-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="sticky top-0 bg-white border-b p-6">
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="text-2xl font-bold text-gray-900">
                      {selectedTask.referenceTaskName}
                    </h3>
                    <p className="text-gray-600 mt-1">{selectedTask.taskType}</p>
                  </div>
                  <button
                    onClick={() => setSelectedTask(null)}
                    className="text-gray-400 hover:text-gray-600"
                  >
                    <span className="text-2xl">✕</span>
                  </button>
                </div>
              </div>

              <div className="p-6 space-y-6">
                {/* Task Info */}
                <div>
                  <h4 className="font-semibold text-gray-900 mb-2">Task Information</h4>
                  <dl className="grid grid-cols-2 gap-4">
                    <div>
                      <dt className="text-sm text-gray-500">Status</dt>
                      <dd className="text-sm font-medium">{selectedTask.status}</dd>
                    </div>
                    <div>
                      <dt className="text-sm text-gray-500">Task ID</dt>
                      <dd className="text-sm font-mono">{selectedTask.taskId}</dd>
                    </div>
                    <div>
                      <dt className="text-sm text-gray-500">Retry Count</dt>
                      <dd className="text-sm">{selectedTask.retryCount}</dd>
                    </div>
                    <div>
                      <dt className="text-sm text-gray-500">Worker ID</dt>
                      <dd className="text-sm font-mono">{selectedTask.workerId || 'N/A'}</dd>
                    </div>
                  </dl>
                </div>

                {/* Input Data */}
                <div>
                  <h4 className="font-semibold text-gray-900 mb-2">Input Data</h4>
                  <pre className="bg-gray-50 p-4 rounded text-xs overflow-auto max-h-60">
                    {JSON.stringify(selectedTask.inputData, null, 2)}
                  </pre>
                </div>

                {/* Output Data */}
                {selectedTask.outputData && Object.keys(selectedTask.outputData).length > 0 && (
                  <div>
                    <h4 className="font-semibold text-gray-900 mb-2">Output Data</h4>
                    <pre className="bg-gray-50 p-4 rounded text-xs overflow-auto max-h-60">
                      {JSON.stringify(selectedTask.outputData, null, 2)}
                    </pre>
                  </div>
                )}

                {/* Error */}
                {selectedTask.reasonForIncompletion && (
                  <div className="bg-red-50 border border-red-200 rounded p-4">
                    <h4 className="font-semibold text-red-800 mb-2">Error Details</h4>
                    <p className="text-sm text-red-700">{selectedTask.reasonForIncompletion}</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

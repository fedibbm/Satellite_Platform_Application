import { useState } from 'react';
import { ConductorWorkflowStatus } from '@/types/conductor';
import { workflowService } from '@/services/workflow.service';
import {
  PlayIcon,
  PauseIcon,
  StopIcon,
  ArrowPathIcon,
  ExclamationTriangleIcon
} from '@heroicons/react/24/outline';

interface ExecutionControlsProps {
  executionId: string;
  status: ConductorWorkflowStatus;
  onStatusChange?: () => void;
  disabled?: boolean;
}

export default function ExecutionControls({ 
  executionId, 
  status, 
  onStatusChange,
  disabled = false 
}: ExecutionControlsProps) {
  const [loading, setLoading] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showTerminateConfirm, setShowTerminateConfirm] = useState(false);

  const handleAction = async (
    action: () => Promise<void>,
    actionName: string
  ) => {
    try {
      setLoading(actionName);
      setError(null);
      await action();
      onStatusChange?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Action failed');
      console.error(`Failed to ${actionName}:`, err);
    } finally {
      setLoading(null);
    }
  };

  const handlePause = () => {
    handleAction(
      () => workflowService.pauseExecution(executionId),
      'pause'
    );
  };

  const handleResume = () => {
    handleAction(
      () => workflowService.resumeExecution(executionId),
      'resume'
    );
  };

  const handleTerminate = () => {
    setShowTerminateConfirm(true);
  };

  const confirmTerminate = () => {
    handleAction(
      () => workflowService.terminateExecution(executionId, 'Terminated by user'),
      'terminate'
    );
    setShowTerminateConfirm(false);
  };

  const handleRetry = () => {
    handleAction(
      () => workflowService.retryExecution(executionId),
      'retry'
    );
  };

  const handleRestart = () => {
    handleAction(
      () => workflowService.restartExecution(executionId),
      'restart'
    );
  };

  const isLoading = (action: string) => loading === action;

  return (
    <div className="space-y-3">
      {/* Action Buttons */}
      <div className="flex flex-wrap gap-2">
        {/* Pause - only for RUNNING */}
        {status === 'RUNNING' && (
          <button
            onClick={handlePause}
            disabled={disabled || loading !== null}
            className="inline-flex items-center gap-2 px-4 py-2 bg-yellow-600 text-white rounded-lg hover:bg-yellow-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isLoading('pause') ? (
              <ArrowPathIcon className="h-5 w-5 animate-spin" />
            ) : (
              <PauseIcon className="h-5 w-5" />
            )}
            <span>Pause</span>
          </button>
        )}

        {/* Resume - only for PAUSED */}
        {status === 'PAUSED' && (
          <button
            onClick={handleResume}
            disabled={disabled || loading !== null}
            className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isLoading('resume') ? (
              <ArrowPathIcon className="h-5 w-5 animate-spin" />
            ) : (
              <PlayIcon className="h-5 w-5" />
            )}
            <span>Resume</span>
          </button>
        )}

        {/* Terminate - only for RUNNING or PAUSED */}
        {(status === 'RUNNING' || status === 'PAUSED') && (
          <button
            onClick={handleTerminate}
            disabled={disabled || loading !== null}
            className="inline-flex items-center gap-2 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isLoading('terminate') ? (
              <ArrowPathIcon className="h-5 w-5 animate-spin" />
            ) : (
              <StopIcon className="h-5 w-5" />
            )}
            <span>Terminate</span>
          </button>
        )}

        {/* Retry - only for FAILED */}
        {status === 'FAILED' && (
          <button
            onClick={handleRetry}
            disabled={disabled || loading !== null}
            className="inline-flex items-center gap-2 px-4 py-2 bg-orange-600 text-white rounded-lg hover:bg-orange-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isLoading('retry') ? (
              <ArrowPathIcon className="h-5 w-5 animate-spin" />
            ) : (
              <ArrowPathIcon className="h-5 w-5" />
            )}
            <span>Retry Failed Tasks</span>
          </button>
        )}

        {/* Restart - for terminal states */}
        {['COMPLETED', 'FAILED', 'TERMINATED', 'TIMED_OUT'].includes(status) && (
          <button
            onClick={handleRestart}
            disabled={disabled || loading !== null}
            className="inline-flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isLoading('restart') ? (
              <ArrowPathIcon className="h-5 w-5 animate-spin" />
            ) : (
              <ArrowPathIcon className="h-5 w-5" />
            )}
            <span>Restart Workflow</span>
          </button>
        )}
      </div>

      {/* Error Display */}
      {error && (
        <div className="flex items-start gap-2 p-3 bg-red-50 border border-red-200 rounded-lg">
          <ExclamationTriangleIcon className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="text-sm font-medium text-red-800">Action Failed</p>
            <p className="text-sm text-red-700 mt-1">{error}</p>
          </div>
          <button
            onClick={() => setError(null)}
            className="text-red-600 hover:text-red-800"
          >
            ✕
          </button>
        </div>
      )}

      {/* Terminate Confirmation Modal */}
      {showTerminateConfirm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4 shadow-xl">
            <div className="flex items-start gap-3 mb-4">
              <div className="flex-shrink-0">
                <ExclamationTriangleIcon className="h-6 w-6 text-red-600" />
              </div>
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-gray-900">
                  Terminate Workflow?
                </h3>
                <p className="mt-2 text-sm text-gray-600">
                  This will immediately stop the workflow execution. This action cannot be undone.
                </p>
              </div>
            </div>
            <div className="flex gap-3 justify-end">
              <button
                onClick={() => setShowTerminateConfirm(false)}
                className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={confirmTerminate}
                disabled={loading !== null}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 transition-colors"
              >
                {isLoading('terminate') ? 'Terminating...' : 'Terminate'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

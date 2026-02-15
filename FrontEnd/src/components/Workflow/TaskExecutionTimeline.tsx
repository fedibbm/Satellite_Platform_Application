import { TaskExecution, TaskStatus, getTaskStatusColor, formatDuration } from '@/types/conductor';
import {
  CheckCircleIcon,
  XCircleIcon,
  ClockIcon,
  ArrowPathIcon,
  ExclamationTriangleIcon,
  MinusCircleIcon,
  ForwardIcon
} from '@heroicons/react/24/outline';

interface TaskExecutionTimelineProps {
  tasks: TaskExecution[];
  onTaskClick?: (task: TaskExecution) => void;
}

export default function TaskExecutionTimeline({ tasks, onTaskClick }: TaskExecutionTimelineProps) {
  const getStatusIcon = (status: TaskStatus) => {
    const colorClass = getTaskStatusColor(status);
    const iconClass = `h-6 w-6 ${colorClass}`;

    switch (status) {
      case 'COMPLETED':
        return <CheckCircleIcon className={iconClass} />;
      case 'FAILED':
      case 'FAILED_WITH_TERMINAL_ERROR':
        return <XCircleIcon className={iconClass} />;
      case 'IN_PROGRESS':
        return <ArrowPathIcon className={`${iconClass} animate-spin`} />;
      case 'SCHEDULED':
        return <ClockIcon className={iconClass} />;
      case 'TIMED_OUT':
        return <ExclamationTriangleIcon className={iconClass} />;
      case 'CANCELED':
        return <MinusCircleIcon className={iconClass} />;
      case 'SKIPPED':
        return <ForwardIcon className={iconClass} />;
      case 'COMPLETED_WITH_ERRORS':
        return <ExclamationTriangleIcon className={iconClass} />;
      default:
        return <ClockIcon className={iconClass} />;
    }
  };

  const getProgressPercentage = () => {
    const completed = tasks.filter(t => 
      ['COMPLETED', 'FAILED', 'FAILED_WITH_TERMINAL_ERROR', 'TIMED_OUT', 'CANCELED', 'SKIPPED', 'COMPLETED_WITH_ERRORS'].includes(t.status)
    ).length;
    return (completed / tasks.length) * 100;
  };

  if (tasks.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500">
        No tasks to display
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Progress bar */}
      <div className="bg-gray-200 rounded-full h-2 overflow-hidden">
        <div 
          className="bg-blue-600 h-full transition-all duration-500"
          style={{ width: `${getProgressPercentage()}%` }}
        />
      </div>

      {/* Task list */}
      <div className="space-y-2">
        {tasks.map((task, index) => (
          <div
            key={task.taskId || index}
            onClick={() => onTaskClick?.(task)}
            className={`flex items-center gap-4 p-4 bg-white rounded-lg border transition-all ${
              onTaskClick ? 'cursor-pointer hover:shadow-md hover:border-blue-300' : ''
            }`}
          >
            {/* Status Icon */}
            <div className="flex-shrink-0">
              {getStatusIcon(task.status)}
            </div>

            {/* Task Info */}
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <h4 className="font-semibold text-gray-900 truncate">
                  {task.referenceTaskName || task.taskType}
                </h4>
                <span className={`px-2 py-0.5 text-xs rounded-full ${getTaskStatusColor(task.status)} bg-opacity-10`}>
                  {task.status}
                </span>
              </div>
              <p className="text-sm text-gray-600">{task.taskType}</p>
              
              {/* Error message */}
              {task.reasonForIncompletion && (
                <p className="mt-2 text-sm text-red-600 bg-red-50 p-2 rounded">
                  {task.reasonForIncompletion}
                </p>
              )}

              {/* Retry info */}
              {task.retryCount > 0 && (
                <p className="mt-1 text-xs text-gray-500">
                  Retry attempt: {task.retryCount}
                </p>
              )}
            </div>

            {/* Timing Info */}
            <div className="flex-shrink-0 text-right text-sm">
              {task.startTime ? (
                <div className="space-y-1">
                  <div className="text-gray-900 font-medium">
                    {formatDuration(task.startTime, task.endTime)}
                  </div>
                  <div className="text-gray-500 text-xs">
                    {new Date(task.startTime).toLocaleTimeString()}
                  </div>
                  {task.endTime && (
                    <div className="text-gray-400 text-xs">
                      to {new Date(task.endTime).toLocaleTimeString()}
                    </div>
                  )}
                </div>
              ) : (
                <div className="text-gray-400">Not started</div>
              )}
            </div>

            {/* Worker Info */}
            {task.workerId && (
              <div className="flex-shrink-0 text-xs text-gray-500">
                Worker: {task.workerId.split('@')[0]}
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Summary */}
      <div className="flex items-center justify-between pt-4 border-t">
        <div className="text-sm text-gray-600">
          Total Tasks: <span className="font-semibold">{tasks.length}</span>
        </div>
        <div className="flex gap-4 text-sm">
          <div className="text-green-600">
            ✓ Completed: {tasks.filter(t => t.status === 'COMPLETED').length}
          </div>
          <div className="text-red-600">
            ✗ Failed: {tasks.filter(t => ['FAILED', 'FAILED_WITH_TERMINAL_ERROR', 'TIMED_OUT'].includes(t.status)).length}
          </div>
          <div className="text-blue-600">
            ⚡ In Progress: {tasks.filter(t => t.status === 'IN_PROGRESS').length}
          </div>
        </div>
      </div>
    </div>
  );
}

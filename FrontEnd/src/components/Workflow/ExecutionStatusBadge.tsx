import { ConductorWorkflowStatus, getStatusColor } from '@/types/conductor';

interface ExecutionStatusBadgeProps {
  status: ConductorWorkflowStatus;
  size?: 'sm' | 'md' | 'lg';
  showIcon?: boolean;
}

export default function ExecutionStatusBadge({ 
  status, 
  size = 'md',
  showIcon = false 
}: ExecutionStatusBadgeProps) {
  const sizeClasses = {
    sm: 'px-2 py-0.5 text-xs',
    md: 'px-2.5 py-1 text-sm',
    lg: 'px-3 py-1.5 text-base'
  };

  const icons: Record<ConductorWorkflowStatus, string> = {
    RUNNING: '⚡',
    COMPLETED: '✓',
    FAILED: '✗',
    TERMINATED: '⊗',
    PAUSED: '⏸',
    TIMED_OUT: '⏱'
  };

  return (
    <span 
      className={`inline-flex items-center gap-1 rounded-full font-medium border ${getStatusColor(status)} ${sizeClasses[size]}`}
    >
      {showIcon && <span>{icons[status]}</span>}
      <span>{status}</span>
    </span>
  );
}

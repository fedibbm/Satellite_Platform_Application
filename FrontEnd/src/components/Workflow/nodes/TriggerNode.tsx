'use client';

import { Handle, Position } from '@xyflow/react';
import { BoltIcon } from '@heroicons/react/24/outline';

export default function TriggerNode({ data }: any) {
  return (
    <div className="bg-purple-50 border-2 border-purple-300 rounded-lg p-4 min-w-[180px]">
      <div className="flex items-center gap-2 mb-2">
        <BoltIcon className="h-5 w-5 text-purple-600" />
        <div className="font-semibold text-purple-900">Trigger</div>
      </div>
      <div className="text-sm text-gray-700">{data.label}</div>
      {data.status && (
        <div className={`mt-2 text-xs px-2 py-1 rounded ${
          data.status === 'running' ? 'bg-blue-100 text-blue-700' :
          data.status === 'success' ? 'bg-green-100 text-green-700' :
          data.status === 'error' ? 'bg-red-100 text-red-700' :
          'bg-gray-100 text-gray-700'
        }`}>
          {data.status}
        </div>
      )}
      <Handle type="source" position={Position.Bottom} className="!bg-purple-500" />
    </div>
  );
}

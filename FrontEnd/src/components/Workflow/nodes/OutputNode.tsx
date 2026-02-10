'use client';

import { Handle, Position } from '@xyflow/react';
import { CloudArrowUpIcon } from '@heroicons/react/24/outline';

export default function OutputNode({ data }: any) {
  return (
    <div className="bg-indigo-50 border-2 border-indigo-300 rounded-lg p-4 min-w-[180px]">
      <div className="flex items-center gap-2 mb-2">
        <CloudArrowUpIcon className="h-5 w-5 text-indigo-600" />
        <div className="font-semibold text-indigo-900">Output</div>
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
      <Handle type="target" position={Position.Top} className="!bg-indigo-500" />
    </div>
  );
}

'use client';

import {
  BoltIcon,
  CloudArrowDownIcon,
  CogIcon,
  QuestionMarkCircleIcon,
  CloudArrowUpIcon,
} from '@heroicons/react/24/outline';

const nodeTypes = [
  { type: 'trigger', label: 'Trigger', icon: BoltIcon, color: 'purple' },
  { type: 'data-input', label: 'Data Input', icon: CloudArrowDownIcon, color: 'blue' },
  { type: 'processing', label: 'Processing', icon: CogIcon, color: 'green' },
  { type: 'decision', label: 'Decision', icon: QuestionMarkCircleIcon, color: 'yellow' },
  { type: 'output', label: 'Output', icon: CloudArrowUpIcon, color: 'indigo' },
];

interface NodePaletteProps {
  onAddNode: (type: string) => void;
}

export default function NodePalette({ onAddNode }: NodePaletteProps) {
  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
      <h3 className="text-sm font-semibold text-gray-700 mb-3">Add Nodes</h3>
      <div className="space-y-2">
        {nodeTypes.map((node) => {
          const Icon = node.icon;
          return (
            <button
              key={node.type}
              onClick={() => onAddNode(node.type)}
              className={`w-full flex items-center gap-2 p-2 rounded hover:bg-${node.color}-50 transition-colors text-left`}
            >
              <Icon className={`h-5 w-5 text-${node.color}-600`} />
              <span className="text-sm text-gray-700">{node.label}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

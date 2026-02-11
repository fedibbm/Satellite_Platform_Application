'use client';

import { useCallback, useEffect } from 'react';
import {
  ReactFlow,
  MiniMap,
  Controls,
  Background,
  addEdge,
  Connection,
  Edge,
  Node,
  useNodesState,
  useEdgesState,
  BackgroundVariant,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { WorkflowNode as WorkflowNodeType, WorkflowEdge as WorkflowEdgeType } from '@/types/workflow';
import TriggerNode from './nodes/TriggerNode';
import DataInputNode from './nodes/DataInputNode';
import ProcessingNode from './nodes/ProcessingNode';
import DecisionNode from './nodes/DecisionNode';
import OutputNode from './nodes/OutputNode';

const nodeTypes = {
  trigger: TriggerNode,
  'data-input': DataInputNode,
  processing: ProcessingNode,
  decision: DecisionNode,
  output: OutputNode,
};

interface WorkflowCanvasProps {
  initialNodes?: WorkflowNodeType[];
  initialEdges?: WorkflowEdgeType[];
  onNodesChange?: (nodes: WorkflowNodeType[]) => void;
  onEdgesChange?: (edges: WorkflowEdgeType[]) => void;
  onNodeClick?: (nodeId: string) => void;
  readOnly?: boolean;
}

export default function WorkflowCanvas({
  initialNodes = [],
  initialEdges = [],
  onNodesChange,
  onEdgesChange,
  onNodeClick,
  readOnly = false,
}: WorkflowCanvasProps) {
  const [nodes, setNodes, onNodesChangeInternal] = useNodesState(initialNodes as Node[]);
  const [edges, setEdges, onEdgesChangeInternal] = useEdgesState(initialEdges as Edge[]);

  // Sync external nodes changes with internal state
  useEffect(() => {
    setNodes(initialNodes as Node[]);
  }, [initialNodes, setNodes]);

  // Sync external edges changes with internal state
  useEffect(() => {
    setEdges(initialEdges as Edge[]);
  }, [initialEdges, setEdges]);

  const handleNodeClick = useCallback(
    (_event: React.MouseEvent, node: Node) => {
      if (!readOnly && onNodeClick) {
        onNodeClick(node.id);
      }
    },
    [readOnly, onNodeClick]
  );

  const onConnect = useCallback(
    (params: Connection | Edge) => {
      if (readOnly) return;
      
      const newEdges = addEdge(params, edges);
      setEdges(newEdges);
      onEdgesChange?.(newEdges as WorkflowEdgeType[]);
    },
    [edges, readOnly, setEdges, onEdgesChange]
  );

  const handleNodesChange = useCallback(
    (changes: any) => {
      onNodesChangeInternal(changes);
      onNodesChange?.(nodes as WorkflowNodeType[]);
    },
    [onNodesChangeInternal, onNodesChange, nodes]
  );

  const handleEdgesChange = useCallback(
    (changes: any) => {
      onEdgesChangeInternal(changes);
      onEdgesChange?.(edges as WorkflowEdgeType[]);
    },
    [onEdgesChangeInternal, onEdgesChange, edges]
  );

  return (
    <div className="w-full h-full">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={handleNodesChange}
        onEdgesChange={handleEdgesChange}
        onConnect={onConnect}
        onNodeClick={handleNodeClick}
        nodeTypes={nodeTypes}
        fitView
        nodesDraggable={!readOnly}
        nodesConnectable={!readOnly}
        elementsSelectable={!readOnly}
      >
        <Controls />
        <MiniMap />
        <Background variant={BackgroundVariant.Dots} gap={12} size={1} />
      </ReactFlow>
    </div>
  );
}

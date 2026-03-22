'use client';

import React, { useCallback, useEffect, useRef } from 'react';
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
  workflowId?: string;
  initialNodes?: WorkflowNodeType[];
  initialEdges?: WorkflowEdgeType[];
  onNodesChange?: (nodes: WorkflowNodeType[]) => void;
  onEdgesChange?: (edges: WorkflowEdgeType[]) => void;
  onNodeClick?: (nodeId: string) => void;
  readOnly?: boolean;
}

export default function WorkflowCanvas({
  workflowId = 'new',
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

  // Auto-Save Draft to LocalStorage
  const draftKey = `workflow_draft_${workflowId}`;
  
  useEffect(() => {
    // Only auto-save if not read-only and we actually have nodes
    if (!readOnly && nodes.length > 0) {
      const draft = {
        nodes,
        edges,
        timestamp: new Date().toISOString()
      };
      // Debounce slightly to avoid aggressive writing
      const timer = setTimeout(() => {
        localStorage.setItem(draftKey, JSON.stringify(draft));
      }, 1000);
      return () => clearTimeout(timer);
    }
  }, [nodes, edges, readOnly, draftKey]);

  // Optionally load draft on mount
  useEffect(() => {
    if (!readOnly && initialNodes.length === 0) {
      const savedDraft = localStorage.getItem(draftKey);
      if (savedDraft) {
        try {
          const { nodes: draftNodes, edges: draftEdges } = JSON.parse(savedDraft);
          if (draftNodes && draftNodes.length > 0) {
             setNodes(draftNodes);
             setEdges(draftEdges || []);
          }
        } catch(e) {
          console.error("Failed to load workflow draft", e);
        }
      }
    }
  }, [readOnly, initialNodes.length, draftKey, setNodes, setEdges]);

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
      if (!isUpdatingFromBackend.current) {
         // Deep clone nodes to break react-flow internal refs
         const currentNodes = [...nodes];
         // Apply changes manually just for the output callback
         onNodesChange?.(currentNodes as WorkflowNodeType[]);
      }
    },
    [onNodesChangeInternal, onNodesChange, nodes]
  );

  // Only sync to parent if the change originated from within ReactFlow UI interactions
  const isUpdatingFromBackend = React.useRef(false);

  useEffect(() => {
    isUpdatingFromBackend.current = true;
    setNodes(initialNodes as Node[]);
    setTimeout(() => { isUpdatingFromBackend.current = false; }, 50);
  }, [initialNodes, setNodes]);

  const handleEdgesChange = useCallback(
    (changes: any) => {
      onEdgesChangeInternal(changes);
    },
    [onEdgesChangeInternal]
  );
  
  useEffect(() => {
    onEdgesChange?.(edges as WorkflowEdgeType[]);
  }, [edges, onEdgesChange]);

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

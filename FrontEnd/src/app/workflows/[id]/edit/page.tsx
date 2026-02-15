'use client';

import { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { workflowService } from '@/services/workflow.service';
import { Workflow } from '@/types/workflow';
import {
  ArrowLeftIcon,
  PlusIcon,
  TrashIcon,
  ArrowUpIcon,
  ArrowDownIcon,
} from '@heroicons/react/24/outline';

interface Task {
  id: string;
  name: string;
  type: string;
  description: string;
  taskReferenceName: string;
  inputParameters: Record<string, any>;
}

const TASK_TYPES = [
  'SIMPLE',
  'HTTP',
  'INLINE',
  'SWITCH',
  'FORK_JOIN',
  'JOIN',
  'SUB_WORKFLOW',
  'EVENT',
  'WAIT',
];

export default function EditWorkflowPage() {
  const router = useRouter();
  const params = useParams();
  const workflowId = params?.id as string;

  const [workflow, setWorkflow] = useState<Workflow | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [tasks, setTasks] = useState<Task[]>([]);

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
      
      // Convert existing nodes to tasks
      if (data.nodes && data.nodes.length > 0) {
        const loadedTasks = data.nodes.map(node => ({
          id: node.id,
          name: node.name || '',
          type: node.type || 'SIMPLE',
          description: node.description || '',
          taskReferenceName: node.taskReferenceName || node.id,
          inputParameters: node.inputParameters || {},
        }));
        setTasks(loadedTasks);
      }
    } catch (error) {
      console.error('Error loading workflow:', error);
    } finally {
      setLoading(false);
    }
  };

  const addTask = () => {
    const newTask: Task = {
      id: `task_${Date.now()}`,
      name: '',
      type: 'SIMPLE',
      description: '',
      taskReferenceName: `task_ref_${Date.now()}`,
      inputParameters: {},
    };
    setTasks([...tasks, newTask]);
  };

  const removeTask = (index: number) => {
    setTasks(tasks.filter((_, i) => i !== index));
  };

  const moveTask = (index: number, direction: 'up' | 'down') => {
    const newTasks = [...tasks];
    const newIndex = direction === 'up' ? index - 1 : index + 1;
    if (newIndex < 0 || newIndex >= tasks.length) return;
    [newTasks[index], newTasks[newIndex]] = [newTasks[newIndex], newTasks[index]];
    setTasks(newTasks);
  };

  const updateTask = (index: number, field: keyof Task, value: any) => {
    const newTasks = [...tasks];
    newTasks[index] = { ...newTasks[index], [field]: value };
    setTasks(newTasks);
  };

  const updateTaskInputParam = (taskIndex: number, paramKey: string, paramValue: string) => {
    const newTasks = [...tasks];
    newTasks[taskIndex].inputParameters = {
      ...newTasks[taskIndex].inputParameters,
      [paramKey]: paramValue,
    };
    setTasks(newTasks);
  };

  const removeTaskInputParam = (taskIndex: number, paramKey: string) => {
    const newTasks = [...tasks];
    const { [paramKey]: _, ...rest } = newTasks[taskIndex].inputParameters;
    newTasks[taskIndex].inputParameters = rest;
    setTasks(newTasks);
  };

  const handleSave = async () => {
    if (tasks.length === 0) {
      alert('Please add at least one task');
      return;
    }

    // Validate tasks
    for (const task of tasks) {
      if (!task.name.trim()) {
        alert('All tasks must have a name');
        return;
      }
      if (!task.taskReferenceName.trim()) {
        alert('All tasks must have a task reference name');
        return;
      }
    }

    try {
      setSaving(true);
      
      // Convert tasks to nodes format
      const nodes = tasks.map((task, index) => ({
        id: task.id,
        type: task.type,
        name: task.name,
        description: task.description,
        taskReferenceName: task.taskReferenceName,
        inputParameters: task.inputParameters,
        position: { x: 100, y: index * 100 },
        configuration: {},
      }));

      // Create edges (sequential flow)
      const edges = tasks.slice(0, -1).map((task, index) => ({
        id: `edge_${task.id}_${tasks[index + 1].id}`,
        source: task.id,
        target: tasks[index + 1].id,
      }));

      await workflowService.updateWorkflow(workflowId, { nodes, edges });
      alert('Workflow saved successfully!');
      router.push(`/workflows/${workflowId}`);
    } catch (error) {
      console.error('Error saving workflow:', error);
      alert('Failed to save workflow');
    } finally {
      setSaving(false);
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
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <button
            onClick={() => router.push(`/workflows/${workflowId}`)}
            className="flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-4"
          >
            <ArrowLeftIcon className="h-5 w-5" />
            Back to Workflow
          </button>
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">Configure Workflow</h1>
              <p className="mt-1 text-sm text-gray-500">{workflow.name}</p>
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => router.push(`/workflows/${workflowId}`)}
                className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
              >
                {saving ? 'Saving...' : 'Save Workflow'}
              </button>
            </div>
          </div>
        </div>

        {/* Tasks List */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold">Workflow Tasks</h2>
            <button
              onClick={addTask}
              className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
            >
              <PlusIcon className="h-5 w-5" />
              Add Task
            </button>
          </div>

          {tasks.length === 0 ? (
            <div className="bg-white rounded-lg p-12 text-center border border-gray-200">
              <p className="text-gray-500 mb-4">No tasks added yet</p>
              <button
                onClick={addTask}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
              >
                Add Your First Task
              </button>
            </div>
          ) : (
            tasks.map((task, index) => (
              <div key={task.id} className="bg-white rounded-lg p-6 border border-gray-200">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <span className="flex items-center justify-center w-8 h-8 bg-blue-100 text-blue-600 rounded-full font-semibold">
                      {index + 1}
                    </span>
                    <h3 className="text-lg font-semibold">Task {index + 1}</h3>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => moveTask(index, 'up')}
                      disabled={index === 0}
                      className="p-1 text-gray-400 hover:text-gray-600 disabled:opacity-30"
                      title="Move up"
                    >
                      <ArrowUpIcon className="h-5 w-5" />
                    </button>
                    <button
                      onClick={() => moveTask(index, 'down')}
                      disabled={index === tasks.length - 1}
                      className="p-1 text-gray-400 hover:text-gray-600 disabled:opacity-30"
                      title="Move down"
                    >
                      <ArrowDownIcon className="h-5 w-5" />
                    </button>
                    <button
                      onClick={() => removeTask(index)}
                      className="p-1 text-red-400 hover:text-red-600"
                      title="Remove task"
                    >
                      <TrashIcon className="h-5 w-5" />
                    </button>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Task Name *
                    </label>
                    <input
                      type="text"
                      value={task.name}
                      onChange={(e) => updateTask(index, 'name', e.target.value)}
                      placeholder="e.g., Process Image"
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Task Type *
                    </label>
                    <select
                      value={task.type}
                      onChange={(e) => updateTask(index, 'type', e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                    >
                      {TASK_TYPES.map((type) => (
                        <option key={type} value={type}>
                          {type}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Task Reference Name *
                    </label>
                    <input
                      type="text"
                      value={task.taskReferenceName}
                      onChange={(e) => updateTask(index, 'taskReferenceName', e.target.value)}
                      placeholder="e.g., process_image_task"
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Description
                    </label>
                    <input
                      type="text"
                      value={task.description}
                      onChange={(e) => updateTask(index, 'description', e.target.value)}
                      placeholder="Task description"
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                </div>

                <div className="mt-4">
                  <div className="flex items-center justify-between mb-2">
                    <label className="block text-sm font-medium text-gray-700">
                      Input Parameters
                    </label>
                    <button
                      onClick={() => {
                        const key = prompt('Enter parameter name:');
                        if (key) updateTaskInputParam(index, key, '');
                      }}
                      className="text-sm text-blue-600 hover:text-blue-700"
                    >
                      + Add Parameter
                    </button>
                  </div>
                  <div className="space-y-2">
                    {Object.entries(task.inputParameters).map(([key, value]) => (
                      <div key={key} className="flex items-center gap-2">
                        <input
                          type="text"
                          value={key}
                          readOnly
                          className="w-1/3 px-3 py-2 border border-gray-300 rounded-lg bg-gray-50"
                        />
                        <input
                          type="text"
                          value={String(value)}
                          onChange={(e) => updateTaskInputParam(index, key, e.target.value)}
                          placeholder="Parameter value"
                          className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                        />
                        <button
                          onClick={() => removeTaskInputParam(index, key)}
                          className="p-2 text-red-400 hover:text-red-600"
                        >
                          <TrashIcon className="h-5 w-5" />
                        </button>
                      </div>
                    ))}
                    {Object.keys(task.inputParameters).length === 0 && (
                      <p className="text-sm text-gray-500 italic">No parameters defined</p>
                    )}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

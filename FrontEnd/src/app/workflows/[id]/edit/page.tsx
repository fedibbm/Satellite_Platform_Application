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
  SparklesIcon,
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
  { value: 'SIMPLE', label: 'Simple Task', description: 'Basic task executed by a worker' },
  { value: 'HTTP', label: 'HTTP Request', description: 'Make HTTP API calls' },
  { value: 'WAIT', label: 'Wait/Delay', description: 'Pause workflow for specified time' },
  { value: 'INLINE', label: 'Inline Script', description: 'Execute JavaScript inline' },
  { value: 'SWITCH', label: 'Switch/Decision', description: 'Conditional branching' },
  { value: 'FORK_JOIN', label: 'Parallel Tasks', description: 'Execute tasks in parallel' },
  { value: 'EVENT', label: 'Event', description: 'Wait for external event' },
];

const TASK_TEMPLATES = {
  SIMPLE: {
    name: 'Simple Task',
    description: 'A basic task that will be executed by a worker',
    inputParameters: {
      message: 'Hello from workflow',
      workerId: 'my-worker'
    }
  },
  HTTP: {
    name: 'HTTP Request',
    description: 'Make an HTTP API call',
    inputParameters: {
      uri: 'http://localhost:9090/api/example',
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    }
  },
  WAIT: {
    name: 'Wait Task',
    description: 'Wait for a specified duration',
    inputParameters: {
      duration: '5s'
    }
  },
  INLINE: {
    name: 'Inline Script',
    description: 'Execute JavaScript code',
    inputParameters: {
      expression: 'function() { return {"result": "success"}; }',
      evaluatorType: 'javascript'
    }
  }
};

export default function EditWorkflowPage() {
  const router = useRouter();
  const params = useParams();
  const workflowId = params?.id as string;

  const [workflow, setWorkflow] = useState<Workflow | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [showTemplates, setShowTemplates] = useState(false);
  const [selectedTaskIndex, setSelectedTaskIndex] = useState<number | null>(null);

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
          name: node.data?.label || node.id,
          type: node.type,
          description: node.data?.description || '',
          taskReferenceName: node.data?.taskReferenceName || node.id,
          inputParameters: node.data?.inputParameters || {}
        }));
        setTasks(loadedTasks);
      }
    } catch (error) {
      console.error('Error loading workflow:', error);
      alert('Failed to load workflow');
    } finally {
      setLoading(false);
    }
  };

  const addTaskFromTemplate = (type: string) => {
    const template = TASK_TEMPLATES[type as keyof typeof TASK_TEMPLATES];
    const taskNumber = tasks.length + 1;
    
    const newTask: Task = {
      id: `task_${Date.now()}`,
      name: template?.name || `Task ${taskNumber}`,
      type: type,
      description: template?.description || '',
      taskReferenceName: `task_${taskNumber}`,
      inputParameters: template?.inputParameters || {}
    };
    
    setTasks([...tasks, newTask]);
    setShowTemplates(false);
    setSelectedTaskIndex(tasks.length); // Expand the newly added task
  };

  const addEmptyTask = () => {
    const taskNumber = tasks.length + 1;
    const newTask: Task = {
      id: `task_${Date.now()}`,
      name: `Task ${taskNumber}`,
      type: 'SIMPLE',
      description: '',
      taskReferenceName: `task_${taskNumber}`,
      inputParameters: {}
    };
    setTasks([...tasks, newTask]);
    setSelectedTaskIndex(tasks.length);
  };

  const removeTask = (index: number) => {
    setTasks(tasks.filter((_, i) => i !== index));
    if (selectedTaskIndex === index) {
      setSelectedTaskIndex(null);
    }
  };

  const moveTask = (index: number, direction: 'up' | 'down') => {
    const newTasks = [...tasks];
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    [newTasks[index], newTasks[targetIndex]] = [newTasks[targetIndex], newTasks[index]];
    setTasks(newTasks);
  };

  const updateTask = (index: number, field: keyof Task, value: any) => {
    const newTasks = [...tasks];
    newTasks[index] = { ...newTasks[index], [field]: value };
    setTasks(newTasks);
  };

  const updateTaskInputParam = (taskIndex: number, key: string, value: any) => {
    const newTasks = [...tasks];
    newTasks[taskIndex].inputParameters = {
      ...newTasks[taskIndex].inputParameters,
      [key]: value
    };
    setTasks(newTasks);
  };

  const addInputParameter = (taskIndex: number) => {
    const key = prompt('Enter parameter name:');
    if (key) {
      updateTaskInputParam(taskIndex, key, '');
    }
  };

  const removeInputParameter = (taskIndex: number, key: string) => {
    const newTasks = [...tasks];
    const { [key]: removed, ...rest } = newTasks[taskIndex].inputParameters;
    newTasks[taskIndex].inputParameters = rest;
    setTasks(newTasks);
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      
      // Convert tasks to nodes and edges
      const nodes = tasks.map((task, index) => ({
        id: task.id,
        type: task.type,
        name: task.name,
        description: task.description,
        taskReferenceName: task.taskReferenceName,
        inputParameters: task.inputParameters,
        position: { x: 100, y: 100 + index * 150 },
        data: {
          label: task.name,
          description: task.description,
          taskReferenceName: task.taskReferenceName,
          inputParameters: task.inputParameters
        }
      }));

      // Create sequential edges
      const edges = tasks.slice(0, -1).map((task, index) => ({
        id: `edge_${task.id}_${tasks[index + 1].id}`,
        source: task.id,
        target: tasks[index + 1].id,
        type: 'default'
      }));

      await workflowService.updateWorkflow(workflowId, {
        ...workflow,
        nodes,
        edges
      });

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

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button
              onClick={() => router.push(`/workflows/${workflowId}`)}
              className="p-2 hover:bg-gray-100 rounded-lg"
            >
              <ArrowLeftIcon className="h-5 w-5" />
            </button>
            <div>
              <h1 className="text-2xl font-bold">{workflow?.name}</h1>
              <p className="text-sm text-gray-600">Configure workflow tasks</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={() => router.push(`/workflows/${workflowId}`)}
              className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              disabled={saving || tasks.length === 0}
              className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {saving ? 'Saving...' : 'Save Tasks'}
            </button>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-5xl mx-auto p-6">
        {/* Add Task Buttons */}
        <div className="mb-6 flex gap-3">
          <button
            onClick={() => setShowTemplates(!showTemplates)}
            className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700"
          >
            <SparklesIcon className="h-5 w-5" />
            Add from Template
          </button>
          <button
            onClick={addEmptyTask}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            <PlusIcon className="h-5 w-5" />
            Add Custom Task
          </button>
        </div>

        {/* Template Selection */}
        {showTemplates && (
          <div className="mb-6 grid grid-cols-2 gap-4 p-4 bg-white rounded-lg border-2 border-purple-200">
            <h3 className="col-span-2 font-semibold text-lg mb-2">Choose a Template</h3>
            {TASK_TYPES.map(taskType => (
              <button
                key={taskType.value}
                onClick={() => addTaskFromTemplate(taskType.value)}
                className="p-4 text-left border border-gray-200 rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-colors"
              >
                <div className="font-semibold text-gray-900">{taskType.label}</div>
                <div className="text-sm text-gray-600 mt-1">{taskType.description}</div>
              </button>
            ))}
          </div>
        )}

        {/* Tasks List */}
        {tasks.length === 0 ? (
          <div className="bg-white rounded-lg border-2 border-dashed border-gray-300 p-12 text-center">
            <SparklesIcon className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">No tasks yet</h3>
            <p className="text-gray-600 mb-4">Add your first task from a template or create a custom one</p>
          </div>
        ) : (
          <div className="space-y-4">
            {tasks.map((task, index) => (
              <div key={task.id} className="bg-white rounded-lg border border-gray-200 overflow-hidden">
                {/* Task Header */}
                <div 
                  className="p-4 flex items-center justify-between cursor-pointer hover:bg-gray-50"
                  onClick={() => setSelectedTaskIndex(selectedTaskIndex === index ? null : index)}
                >
                  <div className="flex items-center gap-3 flex-1">
                    <span className="flex items-center justify-center w-8 h-8 bg-blue-100 text-blue-700 rounded-full font-semibold text-sm">
                      {index + 1}
                    </span>
                    <div className="flex-1">
                      <div className="font-semibold">{task.name}</div>
                      <div className="text-sm text-gray-600">{task.type}</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={(e) => { e.stopPropagation(); moveTask(index, 'up'); }}
                      disabled={index === 0}
                      className="p-1 hover:bg-gray-200 rounded disabled:opacity-30"
                    >
                      <ArrowUpIcon className="h-4 w-4" />
                    </button>
                    <button
                      onClick={(e) => { e.stopPropagation(); moveTask(index, 'down'); }}
                      disabled={index === tasks.length - 1}
                      className="p-1 hover:bg-gray-200 rounded disabled:opacity-30"
                    >
                      <ArrowDownIcon className="h-4 w-4" />
                    </button>
                    <button
                      onClick={(e) => { e.stopPropagation(); removeTask(index); }}
                      className="p-1 hover:bg-red-100 text-red-600 rounded"
                    >
                      <TrashIcon className="h-4 w-4" />
                    </button>
                  </div>
                </div>

                {/* Task Details (Expandable) */}
                {selectedTaskIndex === index && (
                  <div className="border-t border-gray-200 p-4 space-y-4 bg-gray-50">
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          Task Name *
                        </label>
                        <input
                          type="text"
                          value={task.name}
                          onChange={(e) => updateTask(index, 'name', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                          placeholder="e.g., Fetch Data"
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
                          {TASK_TYPES.map(type => (
                            <option key={type.value} value={type.value}>{type.label}</option>
                          ))}
                        </select>
                      </div>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Reference Name *
                      </label>
                      <input
                        type="text"
                        value={task.taskReferenceName}
                        onChange={(e) => updateTask(index, 'taskReferenceName', e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                        placeholder="e.g., fetch_data_task (no spaces)"
                      />
                      <p className="text-xs text-gray-500 mt-1">Used to reference this task's output in other tasks</p>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Description
                      </label>
                      <textarea
                        value={task.description}
                        onChange={(e) => updateTask(index, 'description', e.target.value)}
                        rows={2}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                        placeholder="What does this task do?"
                      />
                    </div>

                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <label className="block text-sm font-medium text-gray-700">
                          Input Parameters
                        </label>
                        <button
                          onClick={() => addInputParameter(index)}
                          className="text-sm text-blue-600 hover:text-blue-700"
                        >
                          + Add Parameter
                        </button>
                      </div>
                      <div className="space-y-2">
                        {Object.entries(task.inputParameters).map(([key, value]) => (
                          <div key={key} className="flex gap-2">
                            <input
                              type="text"
                              value={key}
                              disabled
                              className="w-1/3 px-3 py-2 border border-gray-300 rounded-lg bg-gray-100"
                            />
                            <input
                              type="text"
                              value={typeof value === 'object' ? JSON.stringify(value) : value}
                              onChange={(e) => {
                                try {
                                  const parsed = JSON.parse(e.target.value);
                                  updateTaskInputParam(index, key, parsed);
                                } catch {
                                  updateTaskInputParam(index, key, e.target.value);
                                }
                              }}
                              className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                              placeholder="Value"
                            />
                            <button
                              onClick={() => removeInputParameter(index, key)}
                              className="px-3 py-2 text-red-600 hover:bg-red-50 rounded-lg"
                            >
                              <TrashIcon className="h-4 w-4" />
                            </button>
                          </div>
                        ))}
                        {Object.keys(task.inputParameters).length === 0 && (
                          <p className="text-sm text-gray-500 italic">No parameters yet</p>
                        )}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        {/* Help Text */}
        {tasks.length > 0 && (
          <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <h4 className="font-semibold text-blue-900 mb-2">💡 Tips</h4>
            <ul className="text-sm text-blue-800 space-y-1">
              <li>• Click on a task card to expand and edit its details</li>
              <li>• Tasks will execute in the order shown (top to bottom)</li>
              <li>• Use reference names to access outputs: <code className="bg-blue-100 px-1 rounded">{'${task_name.output.field}'}</code></li>
              <li>• WAIT tasks pause for a duration (e.g., "5s", "1m", "1h")</li>
              <li>• HTTP tasks make API calls - set uri, method, and headers</li>
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}

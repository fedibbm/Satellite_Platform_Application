import re

file_path = '../FrontEnd/src/app/workflows/[id]/page.tsx'
with open(file_path, 'r') as f:
    content = f.read()

# Make sure ws is imported
if 'import { wsService }' not in content:
    content = content.replace("import { Workflow, WorkflowNode, WorkflowEdge } from '@/types/workflow';", "import { Workflow, WorkflowNode, WorkflowEdge } from '@/types/workflow';\nimport { wsService } from '@/services/websocketService';")
    content = content.replace('import { useState, useEffect, useCallback }', 'import { useState, useEffect, useCallback, useRef }')

# Add websocket hook for execution updates
ws_logic = """
  const subscriptionRef = useRef<any>(null);

  useEffect(() => {
    // Reconnect to ws if needed
    if (!wsService.isConnected) {
      wsService.connect();
    }
    
    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
      }
    };
  }, []);

  const subscribeToExecution = (executionId: string) => {
    if (subscriptionRef.current) {
       subscriptionRef.current.unsubscribe();
    }
    
    // Slight delay to ensure connection is ready
    setTimeout(() => {
        subscriptionRef.current = wsService.subscribeToTopic(`/topic/workflow.execution.${executionId}`, (message) => {
           console.log("WebSocket workflow update:", message);
           // Force reload of workflow to fetch updated execution status/nodes
           loadWorkflow(); 
           
           if (message.status === 'COMPLETED' || message.status === 'FAILED') {
               alert(`Workflow execution ${message.status}`);
           } else if (message.status === 'NODE_COMPLETE') {
               // Could show toast or update graph visually here
               console.log("Node completed:", message.data?.nodeId);
           }
        });
    }, 1000);
  };
"""

if 'subscribeToExecution' not in content:
    target = '  const [selectedNode, setSelectedNode] = useState<WorkflowNode | null>(null);'
    content = content.replace(target, target + '\n' + ws_logic)

exec_target = """      await workflowService.executeWorkflow(workflowId);
      alert('Workflow execution started!');
      loadWorkflow(); // Reload to show new execution"""

exec_replace = """      const execResp = await workflowService.executeWorkflow(workflowId);
      alert('Workflow execution started! (Listening for real-time updates)');
      loadWorkflow(); // Reload to show new execution
      
      // Attempt to subscribe using returned execution ID if available
      // The executeWorkflow API returns { status, message, data: WorkflowExecutionDTO } but typings might vary
      if (execResp && execResp.data && execResp.data.id) {
          subscribeToExecution(execResp.data.id);
      } else if (execResp && execResp.id) {
          subscribeToExecution(execResp.id);
      }"""

content = content.replace(exec_target, exec_replace)

with open(file_path, 'w') as f:
    f.write(content)

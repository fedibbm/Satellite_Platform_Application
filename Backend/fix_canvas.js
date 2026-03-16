const fs = require('fs');
const file = '/home/fedi/dev/pfa2/Satellite_Platform_Application/FrontEnd/src/components/Workflow/WorkflowCanvas.tsx';
let data = fs.readFileSync(file, 'utf8');
data = data.replace(
`    const handleNodesChange = useCallback(
      (changes: any) => {
        onNodesChangeInternal(changes);
        onNodesChange?.(nodes as WorkflowNodeType[]);
      },
      [onNodesChangeInternal, onNodesChange, nodes]
    );`,
`    const handleNodesChange = useCallback(
      (changes: any) => {
        onNodesChangeInternal(changes);
        // We must delay sending the state up since 'nodes' state hook hasn't updated in this closure tick
        setTimeout(() => {
            setNodes((currentNodes) => {
                onNodesChange?.(currentNodes as WorkflowNodeType[]);
                return currentNodes;
            });
        }, 0);
      },
      [onNodesChangeInternal, onNodesChange, setNodes]
    );`
);
data = data.replace(
`    const handleEdgesChange = useCallback(
      (changes: any) => {
        onEdgesChangeInternal(changes);
        onEdgesChange?.(edges as WorkflowEdgeType[]);
      },
      [onEdgesChangeInternal, onEdgesChange, edges]
    );`,
`    const handleEdgesChange = useCallback(
      (changes: any) => {
        onEdgesChangeInternal(changes);
        setTimeout(() => {
            setEdges((currentEdges) => {
                onEdgesChange?.(currentEdges as WorkflowEdgeType[]);
                return currentEdges;
            });
        }, 0);
      },
      [onEdgesChangeInternal, onEdgesChange, setEdges]
    );`
)
fs.writeFileSync(file, data);

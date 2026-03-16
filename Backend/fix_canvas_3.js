const fs = require('fs');
const file = '/home/fedi/dev/pfa2/Satellite_Platform_Application/FrontEnd/src/components/Workflow/WorkflowCanvas.tsx';
let data = fs.readFileSync(file, 'utf8');
data = data.replace(
`  const handleNodesChange = useCallback(
    (changes: any) => {
      onNodesChangeInternal(changes);
      onNodesChange?.(nodes as WorkflowNodeType[]);
    },
    [onNodesChangeInternal, onNodesChange, nodes]
  );`,
`  const handleNodesChange = useCallback(
    (changes: any) => {
      onNodesChangeInternal(changes);
    },
    [onNodesChangeInternal]
  );

  useEffect(() => {
    onNodesChange?.(nodes as WorkflowNodeType[]);
  }, [nodes, onNodesChange]);`
);
data = data.replace(
`  const handleEdgesChange = useCallback(
    (changes: any) => {
      onEdgesChangeInternal(changes);
      onEdgesChange?.(edges as WorkflowEdgeType[]);
    },
    [onEdgesChangeInternal, onEdgesChange, edges]
  );`,
`  const handleEdgesChange = useCallback(
    (changes: any) => {
      onEdgesChangeInternal(changes);
    },
    [onEdgesChangeInternal]
  );
  
  useEffect(() => {
    onEdgesChange?.(edges as WorkflowEdgeType[]);
  }, [edges, onEdgesChange]);`
);
fs.writeFileSync(file, data);

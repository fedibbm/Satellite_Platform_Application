const fs = require('fs');
const file = '/home/fedi/dev/pfa2/Satellite_Platform_Application/FrontEnd/src/components/Workflow/WorkflowCanvas.tsx';
let data = fs.readFileSync(file, 'utf8');

data = data.replace(
`  useEffect(() => {
    onNodesChange?.(nodes as WorkflowNodeType[]);
  }, [nodes, onNodesChange]);`,
`  // Only sync to parent if the change originated from within ReactFlow UI interactions
  const isUpdatingFromBackend = React.useRef(false);

  useEffect(() => {
    isUpdatingFromBackend.current = true;
    setNodes(initialNodes as Node[]);
    setTimeout(() => { isUpdatingFromBackend.current = false; }, 50);
  }, [initialNodes, setNodes]);`);

data = data.replace(
`  const handleNodesChange = useCallback(
    (changes: any) => {
      onNodesChangeInternal(changes);
    },
    [onNodesChangeInternal]
  );`,
`  const handleNodesChange = useCallback(
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
  );`
);
// Make sure React is imported for useRef
if(!data.includes("import React, { useCallback, useEffect")) {
    data = data.replace("import { useCallback, useEffect } from 'react';", "import React, { useCallback, useEffect } from 'react';");
}
fs.writeFileSync(file, data);

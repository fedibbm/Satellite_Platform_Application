package com.enit.satellite_platform.modules.workflow.execution;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionContext {
    private String executionId;
    private String workflowId;
    private String userId;
    private Map<String, Object> globalVariables = new HashMap<>();
    private Map<String, Object> nodeOutputs = new HashMap<>();
    private String projectId;
    private Map<String, Object> executionParameters = new HashMap<>();
    
    public void setNodeOutput(String nodeId, Object output) {
        nodeOutputs.put(nodeId, output);
    }
    
    public Object getNodeOutput(String nodeId) {
        return nodeOutputs.get(nodeId);
    }
    
    public void setGlobalVariable(String key, Object value) {
        globalVariables.put(key, value);
    }
    
    public Object getGlobalVariable(String key) {
        return globalVariables.get(key);
    }
}

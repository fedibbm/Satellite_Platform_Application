package com.enit.satellite_platform.modules.workflow.execution;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import java.util.Map;

/**
 * Context passed to each node during execution
 */
public class NodeExecutionContext {
    private final String workflowId;
    private final String executionId;
    private final String userId;
    private final Map<String, Object> globalVariables;
    private final Map<String, Object> nodeOutputs;

    public NodeExecutionContext(String workflowId, String executionId, String userId,
                               Map<String, Object> globalVariables, Map<String, Object> nodeOutputs) {
        this.workflowId = workflowId;
        this.executionId = executionId;
        this.userId = userId;
        this.globalVariables = globalVariables;
        this.nodeOutputs = nodeOutputs;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, Object> getGlobalVariables() {
        return globalVariables;
    }

    public Map<String, Object> getNodeOutputs() {
        return nodeOutputs;
    }

    public Object getNodeOutput(String nodeId) {
        return nodeOutputs.get(nodeId);
    }
}

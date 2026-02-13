package com.enit.satellite_platform.modules.workflow.execution;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NodeExecutorRegistry {
    
    private final Map<String, WorkflowNodeExecutor> executors = new HashMap<>();
    
    public NodeExecutorRegistry(List<WorkflowNodeExecutor> executorList) {
        // Register all executors by their node type
        for (WorkflowNodeExecutor executor : executorList) {
            String nodeType = executor.getNodeType();
            executors.put(nodeType, executor);
            log.info("Registered executor for node type: {}", nodeType);
        }
        
        log.info("Total node executors registered: {}", executors.size());
    }
    
    public WorkflowNodeExecutor getExecutor(String nodeType) {
        WorkflowNodeExecutor executor = executors.get(nodeType);
        if (executor == null) {
            log.warn("No executor found for node type: {}", nodeType);
        }
        return executor;
    }
    
    public boolean hasExecutor(String nodeType) {
        return executors.containsKey(nodeType);
    }
    
    public Map<String, WorkflowNodeExecutor.NodeMetadata> getAllMetadata() {
        Map<String, WorkflowNodeExecutor.NodeMetadata> metadataMap = new HashMap<>();
        executors.forEach((type, executor) -> metadataMap.put(type, executor.getMetadata()));
        return metadataMap;
    }
}

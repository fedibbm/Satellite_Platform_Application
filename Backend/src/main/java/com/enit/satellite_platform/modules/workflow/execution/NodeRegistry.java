package com.enit.satellite_platform.modules.workflow.execution;

import com.enit.satellite_platform.modules.workflow.entities.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for all node executors
 */
@Component
public class NodeRegistry {
    private static final Logger logger = LoggerFactory.getLogger(NodeRegistry.class);
    
    private final Map<NodeType, NodeExecutor> executors = new HashMap<>();

    @Autowired(required = false)
    private List<NodeExecutor> nodeExecutors;

    @PostConstruct
    public void init() {
        logger.info("Initializing Node Registry");
        
        // Auto-register all NodeExecutor beans
        if (nodeExecutors != null) {
            for (NodeExecutor executor : nodeExecutors) {
                register(executor);
            }
        }
        
        logger.info("Node Registry initialized with {} executors", executors.size());
    }

    /**
     * Register a node executor
     */
    public void register(NodeExecutor executor) {
        executors.put(executor.getNodeType(), executor);
        logger.info("Registered executor for node type: {}", executor.getNodeType());
    }

    /**
     * Get executor for a node type
     */
    public Optional<NodeExecutor> getExecutor(NodeType nodeType) {
        return Optional.ofNullable(executors.get(nodeType));
    }

    /**
     * Check if executor exists for a node type
     */
    public boolean hasExecutor(NodeType nodeType) {
        return executors.containsKey(nodeType);
    }

    /**
     * Get all registered executors
     */
    public Map<NodeType, NodeExecutor> getAllExecutors() {
        return new HashMap<>(executors);
    }
}

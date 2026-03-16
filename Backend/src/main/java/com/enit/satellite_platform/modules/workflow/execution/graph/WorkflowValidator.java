package com.enit.satellite_platform.modules.workflow.execution.graph;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.enit.satellite_platform.modules.workflow.entities.NodeType;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowVersion;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class WorkflowValidator {

    public ValidationResult validate(WorkflowVersion version) {
        List<WorkflowNode> nodes = version.getNodes() != null ? version.getNodes() : Collections.emptyList();
        List<WorkflowEdge> edges = version.getEdges() != null ? version.getEdges() : Collections.emptyList();

        if (nodes.isEmpty()) {
            return ValidationResult.error("Workflow has no nodes. At least one TRIGGER node is required.");
        }

        // 1. Check for exactly one or more Trigger nodes
        long triggerCount = nodes.stream().filter(n -> n.getType() == NodeType.TRIGGER).count();
        if (triggerCount == 0) {
            return ValidationResult.error("Workflow must have at least one TRIGGER node.");
        }

        // 2. Validate Edges connect existing nodes
        Set<String> nodeIds = nodes.stream().map(WorkflowNode::getId).collect(Collectors.toSet());
        for (WorkflowEdge edge : edges) {
            if (!nodeIds.contains(edge.getSource()) || !nodeIds.contains(edge.getTarget())) {
                return ValidationResult.error("Workflow contains disjointed or dangling edges pointing to missing nodes.");
            }
        }

        // 3. Cycle Detection (DAG Validation) using DFS
        if (hasCycles(nodes, edges)) {
            return ValidationResult.error("Workflow contains an infinite loop (cycle). Workflows must be a Directed Acyclic Graph (DAG).");
        }

        return ValidationResult.success();
    }

    private boolean hasCycles(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        // Build adjacency list
        Map<String, List<String>> adjList = new HashMap<>();
        for (WorkflowNode node : nodes) {
            adjList.put(node.getId(), new ArrayList<>());
        }
        for (WorkflowEdge edge : edges) {
            adjList.get(edge.getSource()).add(edge.getTarget());
        }

        // State tracking
        // 0 = unvisited, 1 = visiting (in current path), 2 = visited (fully checked)
        Map<String, Integer> state = new HashMap<>();
        for (WorkflowNode node : nodes) {
            state.put(node.getId(), 0);
        }

        for (WorkflowNode node : nodes) {
            if (state.get(node.getId()) == 0) {
                if (dfsCheckCycle(node.getId(), adjList, state)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean dfsCheckCycle(String current, Map<String, List<String>> adjList, Map<String, Integer> state) {
        state.put(current, 1); // Mark as visiting

        for (String neighbor : adjList.get(current)) {
            if (state.get(neighbor) == 1) {
                return true; // Found a back-edge indicating a cycle
            }
            if (state.get(neighbor) == 0) {
                if (dfsCheckCycle(neighbor, adjList, state)) {
                    return true;
                }
            }
        }

        state.put(current, 2); // Mark as fully visited
        return false;
    }

    public static class ValidationResult {
        private final boolean isValid;
        private final String errorMessage;

        private ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() { return isValid; }
        public String getErrorMessage() { return errorMessage; }
    }
}

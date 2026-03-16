package com.enit.satellite_platform.modules.workflow.execution.graph;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowVersion;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ExecutionPlanner {

    /**
     * Determines the correct, ordered execution batches based on dependency graph (Topological Sort rules).
     * Returns a list of "stages".
     * Nodes in the same stage can be executed in parallel (or arbitrarily sequenced if not parallelized).
     * A stage will only execute AFTER the previous stage has fully finished.
     */
    public List<List<WorkflowNode>> planExecution(WorkflowVersion version) {
        List<WorkflowNode> nodes = version.getNodes() != null ? version.getNodes() : Collections.emptyList();
        List<WorkflowEdge> edges = version.getEdges() != null ? version.getEdges() : Collections.emptyList();

        if (nodes.isEmpty()) {
            return Collections.emptyList();
        }

        // Map Node ID -> Node Object for easy lookup
        Map<String, WorkflowNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(WorkflowNode::getId, n -> n));

        // Build adjacency list (forward dependencies) and calculate in-degrees (number of required inputs)
        Map<String, List<String>> adjList = new HashMap<>();
        Map<String, Integer> inDegrees = new HashMap<>();

        for (WorkflowNode node : nodes) {
            adjList.put(node.getId(), new ArrayList<>());
            inDegrees.put(node.getId(), 0);
        }

        for (WorkflowEdge edge : edges) {
            adjList.get(edge.getSource()).add(edge.getTarget());
            // Increase the requirement count for the target node
            inDegrees.put(edge.getTarget(), inDegrees.get(edge.getTarget()) + 1);
        }

        // Start queue with nodes that have exactly 0 dependencies (Triggers/Sources)
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegrees.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<List<WorkflowNode>> executionPlan = new ArrayList<>();

        // Kahn's Algorithm for Topological Sort but grouped by depth levels
        while (!queue.isEmpty()) {
            int currentLevelSize = queue.size();
            List<WorkflowNode> currentExecutionStage = new ArrayList<>();

            for (int i = 0; i < currentLevelSize; i++) {
                String currentNodeId = queue.poll();
                currentExecutionStage.add(nodeMap.get(currentNodeId));

                // Process children
                for (String childId : adjList.get(currentNodeId)) {
                    inDegrees.put(childId, inDegrees.get(childId) - 1);
                    // If a child has all its incoming edges resolved, it's ready for the NEXT stage
                    if (inDegrees.get(childId) == 0) {
                        queue.offer(childId);
                    }
                }
            }

            executionPlan.add(currentExecutionStage);
        }

        return executionPlan;
    }
}

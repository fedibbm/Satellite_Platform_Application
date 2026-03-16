import re

with open('src/main/java/com/enit/satellite_platform/modules/workflow/services/WorkflowExecutionService.java', 'r') as f:
    content = f.read()

target = """                try {
                    logger.info("Executing node: {} of type: {}", node.getId(), node.getType());"""

replacement = """                try {
                    // --- SKIPPING LOGIC FOR DECISION NODES ---
                    boolean shouldSkip = false;
                    
                    // If it has incoming edges, check if they are all active
                    List<com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge> inboundEdges = edges.stream()
                            .filter(e -> e.getTarget().equals(node.getId()))
                            .collect(Collectors.toList());

                    if (!inboundEdges.isEmpty()) {
                        boolean hasActiveInbound = false;
                        for (com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge edge : inboundEdges) {
                            String sourceId = edge.getSource();
                            
                            // Check if source was skipped
                            Object sourceSkipped = context.getGlobalVariables().get(sourceId + ".skipped");
                            if (Boolean.TRUE.equals(sourceSkipped)) {
                                continue;
                            }
                            
                            // Check if source was a decision that evaluated differently
                            Object decisionResult = context.getGlobalVariables().get(sourceId + ".decision");
                            if (decisionResult != null) {
                                String expectedLabel = edge.getLabel() != null ? edge.getLabel().toLowerCase() : "";
                                // The Decision node returns Boolean decision. We convert "true"/"false" and match
                                String decisionStr = String.valueOf(decisionResult).toLowerCase();
                                if (!expectedLabel.isEmpty() && !expectedLabel.equals(decisionStr)) {
                                    continue;
                                }
                            }
                            
                            hasActiveInbound = true;
                            break;
                        }
                        
                        if (!hasActiveInbound) {
                            shouldSkip = true;
                        }
                    }

                    if (shouldSkip) {
                        logger.info("Skipping node {} due to conditional routing", node.getId());
                        context.getGlobalVariables().put(node.getId() + ".skipped", true);
                        addLog(execution, node.getId(), LogLevel.INFO, "Skipped node execution due to conditional routing");
                        continue;
                    }

                    logger.info("Executing node: {} of type: {}", node.getId(), node.getType());"""

new_content = content.replace(target, replacement)

with open('src/main/java/com/enit/satellite_platform/modules/workflow/services/WorkflowExecutionService.java', 'w') as f:
    f.write(new_content)

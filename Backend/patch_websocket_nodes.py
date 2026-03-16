import re

file_path = 'src/main/java/com/enit/satellite_platform/modules/workflow/services/WorkflowExecutionService.java'
with open(file_path, 'r') as f:
    content = f.read()

# Broadcast node success
node_success_t = "                        // Log success"
node_success_r = "                        notifyStatusUpdate(execution.getId(), \"NODE_COMPLETE\", \"Node completed\", Map.of(\"nodeId\", node.getId(), \"result\", result.getData()));\n" + \
                 "                        // Log success"
if 'notifyStatusUpdate(execution.getId(), "NODE_COMPLETE"' not in content:
    content = content.replace(node_success_t, node_success_r)

node_start_t = "                    logger.info(\"Executing node: {} of type: {}\", node.getId(), node.getType());"
node_start_r = "                    notifyStatusUpdate(execution.getId(), \"NODE_START\", \"Executing node\", Map.of(\"nodeId\", node.getId()));\n" + \
               "                    logger.info(\"Executing node: {} of type: {}\", node.getId(), node.getType());"
if 'notifyStatusUpdate(execution.getId(), "NODE_START"' not in content:
    content = content.replace(node_start_t, node_start_r)

with open(file_path, 'w') as f:
    f.write(content)

import re

file_path = 'src/main/java/com/enit/satellite_platform/modules/workflow/services/WorkflowExecutionService.java'
with open(file_path, 'r') as f:
    content = f.read()

# Add import
if 'org.springframework.messaging.simp.SimpMessagingTemplate' not in content:
    content = content.replace('import org.springframework.stereotype.Service;', 'import org.springframework.stereotype.Service;\nimport org.springframework.messaging.simp.SimpMessagingTemplate;')

# Add injected member
if 'SimpMessagingTemplate messagingTemplate' not in content:
    inject_target = '@Autowired\n    private RabbitTemplate rabbitTemplate;'
    inject_replacement = '@Autowired\n    private RabbitTemplate rabbitTemplate;\n\n    @Autowired\n    private SimpMessagingTemplate messagingTemplate;'
    content = content.replace(inject_target, inject_replacement)

# Create a notifyStatus function
notify_func = """
    private void notifyStatusUpdate(String executionId, String status, String message, Object data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("executionId", executionId);
            payload.put("status", status);
            payload.put("message", message);
            if (data != null) payload.put("data", data);
            
            // Broadcast to the specific workflow execution topic
            messagingTemplate.convertAndSend("/topic/workflow.execution." + executionId, payload);
        } catch (Exception e) {
            logger.warn("Failed to broadcast execution status update", e);
        }
    }
"""

if 'private void notifyStatusUpdate' not in content:
    # Add just before the last closing brace
    content = content[:content.rfind('}')] + notify_func + '\n}'

# Update setExecutionStatus to call the broadcaster
target_status = "        execution.setStatus(status);\n        return executionRepository.save(execution);"
replace_status = "        execution.setStatus(status);\n" + \
                 "        WorkflowExecution saved = executionRepository.save(execution);\n" + \
                 "        notifyStatusUpdate(saved.getId(), status.name(), \"Workflow execution state changed\", null);\n" + \
                 "        return saved;"

if 'notifyStatusUpdate(saved.getId()' not in content:
    content = content.replace(target_status, replace_status)

with open(file_path, 'w') as f:
    f.write(content)

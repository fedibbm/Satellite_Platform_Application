import re

with open('src/main/java/com/enit/satellite_platform/modules/workflow/services/WorkflowExecutionService.java', 'r') as f:
    content = f.read()

# We want to replace the try block in executeWorkflow
target_block = """        // Execute workflow asynchronously (for now, just mark as completed)
        // In a real implementation, this would trigger the actual execution
        try {
            executeWorkflowNodes(savedExecution, currentVersion);
            logger.info("executeWorkflowNodes completed for workflow: {}", workflowId);
        } catch (Exception e) {
            logger.error("Uncaught exception in executeWorkflowNodes for workflow: {} - Type: {}, Message: {}", 
                workflowId, e.getClass().getSimpleName(), e.getMessage(), e);
            savedExecution.setStatus(ExecutionStatus.FAILED);
            WorkflowLog errorLog = new WorkflowLog();
            errorLog.setTimestamp(LocalDateTime.now());
            errorLog.setNodeId("system");
            errorLog.setLevel(LogLevel.ERROR);
            String errorMessage = String.format("Workflow execution failed: %s - %s", 
                e.getClass().getSimpleName(), e.getMessage());
            errorLog.setMessage(errorMessage);
            savedExecution.getLogs().add(errorLog);
            savedExecution.setCompletedAt(LocalDateTime.now());
            executionRepository.save(savedExecution);
            logger.info("Saved execution with FAILED status after uncaught exception");
        }"""

replacement = """        // Submit to RabbitMQ for asynchronous execution
        rabbitTemplate.convertAndSend(WorkflowRabbitMQConfig.WORKFLOW_EXECUTION_QUEUE, savedExecution.getId());
        logger.info("Workflow execution {} submitted to RabbitMQ for processing", savedExecution.getId());"""

content = content.replace(target_block, replacement)

listener_code = """
    @RabbitListener(queues = WorkflowRabbitMQConfig.WORKFLOW_EXECUTION_QUEUE)
    public void processWorkflowExecutionQueue(String executionId) {
        logger.info("Received executionId {} from queue", executionId);
        try {
            WorkflowExecution execution = executionRepository.findById(executionId).orElse(null);
            if (execution == null) {
                logger.error("Execution not found: {}", executionId);
                return;
            }
            
            Workflow workflow = workflowRepository.findById(execution.getWorkflowId()).orElse(null);
            if (workflow == null) {
                logger.error("Workflow not found: {}", execution.getWorkflowId());
                return;
            }
            
            WorkflowVersion version = workflow.getVersions().stream()
                    .filter(v -> v.getVersion().equals(execution.getVersion()))
                    .findFirst()
                    .orElse(null);
                    
            if (version == null) {
                logger.error("Version {} not found for workflow {}", execution.getVersion(), workflow.getId());
                return;
            }
            
            executeWorkflowNodes(execution, version);
        } catch (Exception e) {
            logger.error("Failed to process execution {} from queue", executionId, e);
        }
    }
"""

if "processWorkflowExecutionQueue" not in content:
    # insert before executeWorkflowNodes
    content = content.replace("private void executeWorkflowNodes(", listener_code + "\n    private void executeWorkflowNodes(")

with open('src/main/java/com/enit/satellite_platform/modules/workflow/services/WorkflowExecutionService.java', 'w') as f:
    f.write(content)

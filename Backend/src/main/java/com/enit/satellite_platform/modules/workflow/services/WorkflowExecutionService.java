package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowExecution;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowLog;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowExecutionService {
    
    private final WorkflowExecutionRepository executionRepository;
    
    public WorkflowExecution createExecution(String workflowId, String version, String userId, Map<String, Object> parameters) {
        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowId(workflowId);
        execution.setVersion(version);
        execution.setStatus("pending");
        execution.setTriggeredBy(userId);
        execution.setStartedAt(LocalDateTime.now());
        execution.setResult(parameters != null ? parameters : new HashMap<>());
        
        return executionRepository.save(execution);
    }
    
    public WorkflowExecution getExecutionById(String id) {
        return executionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Execution not found with id: " + id));
    }
    
    public List<WorkflowExecution> getExecutionsByWorkflowId(String workflowId) {
        return executionRepository.findByWorkflowIdOrderByStartedAtDesc(workflowId);
    }
    
    public List<WorkflowExecution> getAllExecutions() {
        return executionRepository.findAll();
    }
    
    public WorkflowExecution updateExecutionStatus(String executionId, String status) {
        WorkflowExecution execution = getExecutionById(executionId);
        execution.setStatus(status);
        
        if ("completed".equals(status) || "failed".equals(status) || "cancelled".equals(status)) {
            execution.setCompletedAt(LocalDateTime.now());
        }
        
        return executionRepository.save(execution);
    }
    
    public WorkflowExecution addLog(String executionId, String nodeId, String level, String message) {
        WorkflowExecution execution = getExecutionById(executionId);
        
        WorkflowLog log = new WorkflowLog();
        log.setTimestamp(LocalDateTime.now());
        log.setNodeId(nodeId);
        log.setLevel(level);
        log.setMessage(message);
        
        execution.getLogs().add(log);
        return executionRepository.save(execution);
    }
    
    public WorkflowExecution updateExecutionResult(String executionId, Map<String, Object> result) {
        WorkflowExecution execution = getExecutionById(executionId);
        execution.setResult(result);
        return executionRepository.save(execution);
    }
    
    public WorkflowExecution setExecutionError(String executionId, String errorMessage) {
        WorkflowExecution execution = getExecutionById(executionId);
        execution.setStatus("failed");
        execution.setErrorMessage(errorMessage);
        execution.setCompletedAt(LocalDateTime.now());
        return executionRepository.save(execution);
    }
}

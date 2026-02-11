package com.enit.satellite_platform.modules.workflow.mapper;

import com.enit.satellite_platform.modules.workflow.dto.WorkflowDTO;
import com.enit.satellite_platform.modules.workflow.dto.WorkflowExecutionDTO;
import com.enit.satellite_platform.modules.workflow.entities.Workflow;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowExecution;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WorkflowMapper {

    public WorkflowDTO toDTO(Workflow workflow) {
        if (workflow == null) {
            return null;
        }

        WorkflowDTO dto = new WorkflowDTO();
        dto.setId(workflow.getId());
        dto.setName(workflow.getName());
        dto.setDescription(workflow.getDescription());
        dto.setStatus(workflow.getStatus());
        dto.setProjectId(workflow.getProjectId() != null ? workflow.getProjectId().toHexString() : null);
        dto.setCurrentVersion(workflow.getCurrentVersion());
        dto.setVersions(workflow.getVersions());
        dto.setExecutions(new ArrayList<>()); // Will be populated separately if needed
        dto.setCreatedAt(workflow.getCreatedAt());
        dto.setUpdatedAt(workflow.getUpdatedAt());
        dto.setCreatedBy(workflow.getCreatedBy());
        dto.setTags(workflow.getTags());
        dto.setIsTemplate(workflow.getIsTemplate());

        return dto;
    }

    public WorkflowDTO toDTOWithExecutions(Workflow workflow, List<WorkflowExecution> executions) {
        WorkflowDTO dto = toDTO(workflow);
        if (dto != null && executions != null) {
            dto.setExecutions(executions.stream()
                    .map(this::toExecutionDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public WorkflowExecutionDTO toExecutionDTO(WorkflowExecution execution) {
        if (execution == null) {
            return null;
        }

        WorkflowExecutionDTO dto = new WorkflowExecutionDTO();
        dto.setId(execution.getId());
        dto.setWorkflowId(execution.getWorkflowId());
        dto.setVersion(execution.getVersion());
        dto.setStatus(execution.getStatus());
        dto.setStartedAt(execution.getStartedAt());
        dto.setCompletedAt(execution.getCompletedAt());
        dto.setTriggeredBy(execution.getTriggeredBy());
        dto.setLogs(execution.getLogs());
        dto.setResults(execution.getResults());

        return dto;
    }

    public List<WorkflowDTO> toDTOList(List<Workflow> workflows) {
        if (workflows == null) {
            return new ArrayList<>();
        }
        return workflows.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}

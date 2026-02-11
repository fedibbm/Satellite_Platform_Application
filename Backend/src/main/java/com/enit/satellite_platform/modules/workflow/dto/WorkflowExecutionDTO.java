package com.enit.satellite_platform.modules.workflow.dto;

import com.enit.satellite_platform.modules.workflow.entities.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class WorkflowExecutionDTO {
    private String id;
    private String workflowId;
    private String version;
    private ExecutionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String triggeredBy;
    private List<WorkflowLog> logs;
    private Map<String, Object> results;
}

package com.enit.satellite_platform.modules.workflow.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "workflow_executions")
public class WorkflowExecution {
    @Id
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

package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workflow_executions")
public class WorkflowExecution {
    @Id
    private String id;
    private String workflowId;
    private String version;
    private String status; // pending, running, completed, failed, cancelled
    private String triggeredBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<WorkflowLog> logs = new ArrayList<>();
    private Map<String, Object> result = new HashMap<>();
    private String errorMessage;
}

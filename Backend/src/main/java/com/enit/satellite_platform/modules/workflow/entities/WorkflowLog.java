package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowLog {
    private LocalDateTime timestamp;
    private String nodeId;
    private String level; // INFO, WARNING, ERROR
    private String message;
}

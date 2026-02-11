package com.enit.satellite_platform.modules.workflow.entities;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkflowLog {
    private LocalDateTime timestamp;
    private String nodeId;
    private LogLevel level;
    private String message;
}

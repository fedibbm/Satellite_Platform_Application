package com.enit.satellite_platform.modules.workflow.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.bson.types.ObjectId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "workflows")
public class Workflow {
    @Id
    private String id;
    private String name;
    private String description;
    private WorkflowStatus status;
    private ObjectId projectId;
    private String currentVersion;
    private List<WorkflowVersion> versions;
    private List<String> executionIds; // Reference to execution IDs
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private List<String> tags;
    private Boolean isTemplate;

    public Workflow() {
        this.versions = new ArrayList<>();
        this.executionIds = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.isTemplate = false;
        this.status = WorkflowStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}

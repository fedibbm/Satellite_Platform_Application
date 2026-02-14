package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Workflow Definition Entity
 * Stores workflow metadata and structure in MongoDB
 * Will be translated to Conductor workflow format during execution
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workflow_definitions")
public class WorkflowDefinition {
    
    @Id
    private String id;
    
    private String name;
    
    private String description;
    
    private String projectId;
    
    private String version;
    
    private String createdBy;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private String status; // DRAFT, PUBLISHED, ARCHIVED
    
    private List<WorkflowNode> nodes = new ArrayList<>();
    
    private List<WorkflowEdge> edges = new ArrayList<>();
    
    private WorkflowMetadata metadata;
}

package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.dto.*;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowDefinition;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowMetadata;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing workflow definitions (CRUD operations)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowDefinitionService {
    
    private final WorkflowDefinitionRepository workflowRepository;
    
    /**
     * Create a new workflow definition
     */
    public WorkflowResponse createWorkflow(CreateWorkflowRequest request, String userId) {
        log.info("Creating new workflow: {} for project: {}", request.getName(), request.getProjectId());
        
        // Check if workflow with same name exists in project
        workflowRepository.findByNameAndProjectId(request.getName(), request.getProjectId())
                .ifPresent(existing -> {
                    throw new RuntimeException("Workflow with name '" + request.getName() + 
                            "' already exists in project " + request.getProjectId());
                });
        
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setProjectId(request.getProjectId());
        workflow.setVersion("1.0");
        workflow.setStatus("DRAFT");
        workflow.setCreatedBy(userId);
        workflow.setCreatedAt(LocalDateTime.now());
        workflow.setUpdatedAt(LocalDateTime.now());
        workflow.setNodes(request.getNodes());
        workflow.setEdges(request.getEdges());
        
        // Set metadata
        WorkflowMetadata metadata = new WorkflowMetadata();
        metadata.setTimeoutSeconds(request.getTimeoutSeconds());
        metadata.setTags(request.getTags());
        metadata.setSchemaVersion("1.0");
        metadata.setRestartable(true);
        workflow.setMetadata(metadata);
        
        WorkflowDefinition saved = workflowRepository.save(workflow);
        log.info("Workflow created successfully with id: {}", saved.getId());
        
        return toResponse(saved);
    }
    
    /**
     * Get workflow by ID
     */
    public WorkflowResponse getWorkflowById(String id) {
        // Check if it's a template ID
        if (id != null && id.startsWith("template_")) {
            return getWorkflowTemplates().stream()
                    .filter(template -> id.equals(template.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));
        }
        
        WorkflowDefinition workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found with id: " + id));
        return toResponse(workflow);
    }
    
    /**
     * Get all workflows
     */
    public List<WorkflowResponse> getAllWorkflows() {
        return workflowRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get workflows by project ID
     */
    public List<WorkflowResponse> getWorkflowsByProject(String projectId) {
        return workflowRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get workflows by status
     */
    public List<WorkflowResponse> getWorkflowsByStatus(String status) {
        return workflowRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get workflow templates (pre-configured workflow examples)
     */
    public List<WorkflowResponse> getWorkflowTemplates() {
        return List.of(
            createSatelliteProcessingTemplate(),
            createSimpleTwoTaskTemplate(),
            createFullPipelineTemplate()
        );
    }
    
    private WorkflowResponse createSatelliteProcessingTemplate() {
        WorkflowResponse template = new WorkflowResponse();
        template.setId("template_satellite_processing");
        template.setName("Satellite Image Processing Workflow");
        template.setDescription("Complete workflow for fetching, processing and storing satellite imagery");
        template.setProjectId("template_project");
        template.setVersion("1.0");
        template.setStatus("TEMPLATE");
        template.setCreatedBy("system");
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        template.setTimeoutSeconds(3600);
        template.setTags(new String[]{"satellite", "ndvi", "gee", "processing", "template"});
        
        // Create nodes
        List<com.enit.satellite_platform.modules.workflow.entities.WorkflowNode> nodes = new java.util.ArrayList<>();
        
        // Node 1: Trigger
        var triggerNode = new com.enit.satellite_platform.modules.workflow.entities.WorkflowNode();
        triggerNode.setId("node_trigger_1");
        triggerNode.setType("TRIGGER");
        triggerNode.setName("Workflow Start");
        triggerNode.setDescription("Initial trigger to start the workflow");
        triggerNode.setTaskReferenceName("workflow_trigger_start");
        var triggerPos = new com.enit.satellite_platform.modules.workflow.entities.NodePosition();
        triggerPos.setX(100.0); triggerPos.setY(100.0);
        triggerNode.setPosition(triggerPos);
        var triggerConfig = new com.enit.satellite_platform.modules.workflow.entities.NodeConfiguration();
        triggerConfig.setTaskType("SIMPLE");
        triggerConfig.setTaskName("workflow_trigger");
        triggerConfig.setTimeoutSeconds(60);
        triggerNode.setConfiguration(triggerConfig);
        triggerNode.setInputParameters(new java.util.HashMap<>());
        nodes.add(triggerNode);
        
        // Node 2: Load Image
        var loadNode = new com.enit.satellite_platform.modules.workflow.entities.WorkflowNode();
        loadNode.setId("node_gee_load_2");
        loadNode.setType("TASK");
        loadNode.setName("Load Satellite Image");
        loadNode.setDescription("Fetch satellite image from Google Earth Engine");
        loadNode.setTaskReferenceName("load_image");
        var loadPos = new com.enit.satellite_platform.modules.workflow.entities.NodePosition();
        loadPos.setX(300.0); loadPos.setY(100.0);
        loadNode.setPosition(loadPos);
        var loadConfig = new com.enit.satellite_platform.modules.workflow.entities.NodeConfiguration();
        loadConfig.setTaskType("SIMPLE");
        loadConfig.setTaskName("load_image");
        loadConfig.setTimeoutSeconds(600);
        var loadRetry = new com.enit.satellite_platform.modules.workflow.entities.RetryConfig();
        loadRetry.setRetryCount(3);
        loadRetry.setRetryDelaySeconds(10);
        loadRetry.setRetryLogic("FIXED");
        loadConfig.setRetryConfig(loadRetry);
        loadNode.setConfiguration(loadConfig);
        var loadParams = new java.util.HashMap<String, Object>();
        loadParams.put("imageId", "${workflow.input.imageId}");
        loadParams.put("region", "${workflow.input.region}");
        loadParams.put("projectId", "${workflow.input.projectId}");
        loadNode.setInputParameters(loadParams);
        nodes.add(loadNode);
        
        // Node 3: NDVI Processing
        var ndviNode = new com.enit.satellite_platform.modules.workflow.entities.WorkflowNode();
        ndviNode.setId("node_ndvi_3");
        ndviNode.setType("TASK");
        ndviNode.setName("Calculate NDVI");
        ndviNode.setDescription("Calculate Normalized Difference Vegetation Index");
        ndviNode.setTaskReferenceName("calculate_ndvi");
        var ndviPos = new com.enit.satellite_platform.modules.workflow.entities.NodePosition();
        ndviPos.setX(500.0); ndviPos.setY(100.0);
        ndviNode.setPosition(ndviPos);
        var ndviConfig = new com.enit.satellite_platform.modules.workflow.entities.NodeConfiguration();
        ndviConfig.setTaskType("SIMPLE");
        ndviConfig.setTaskName("calculate_ndvi");
        ndviConfig.setTimeoutSeconds(600);
        var ndviRetry = new com.enit.satellite_platform.modules.workflow.entities.RetryConfig();
        ndviRetry.setRetryCount(2);
        ndviRetry.setRetryDelaySeconds(15);
        ndviRetry.setRetryLogic("FIXED");
        ndviConfig.setRetryConfig(ndviRetry);
        ndviNode.setConfiguration(ndviConfig);
        var ndviParams = new java.util.HashMap<String, Object>();
        ndviParams.put("imageData", "${load_image.output.imageData}");
        ndviParams.put("redBand", "B4");
        ndviParams.put("nirBand", "B8");
        ndviNode.setInputParameters(ndviParams);
        nodes.add(ndviNode);
        
        // Node 4: Storage
        var storageNode = new com.enit.satellite_platform.modules.workflow.entities.WorkflowNode();
        storageNode.setId("node_storage_4");
        storageNode.setType("TASK");
        storageNode.setName("Store Results");
        storageNode.setDescription("Save processed results to storage");
        storageNode.setTaskReferenceName("save_results");
        var storagePos = new com.enit.satellite_platform.modules.workflow.entities.NodePosition();
        storagePos.setX(700.0); storagePos.setY(100.0);
        storageNode.setPosition(storagePos);
        var storageConfig = new com.enit.satellite_platform.modules.workflow.entities.NodeConfiguration();
        storageConfig.setTaskType("SIMPLE");
        storageConfig.setTaskName("save_results");
        storageConfig.setTimeoutSeconds(300);
        var storageRetry = new com.enit.satellite_platform.modules.workflow.entities.RetryConfig();
        storageRetry.setRetryCount(3);
        storageRetry.setRetryDelaySeconds(5);
        storageRetry.setRetryLogic("FIXED");
        storageConfig.setRetryConfig(storageRetry);
        storageNode.setConfiguration(storageConfig);
        var storageParams = new java.util.HashMap<String, Object>();
        storageParams.put("data", "${calculate_ndvi.output.result}");
        storageParams.put("projectId", "${workflow.input.projectId}");
        storageParams.put("userId", "${workflow.input.userId}");
        storageParams.put("storageType", "database");
        storageNode.setInputParameters(storageParams);
        nodes.add(storageNode);
        
        template.setNodes(nodes);
        
        // Create edges
        List<com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge> edges = new java.util.ArrayList<>();
        
        var edge1 = new com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge();
        edge1.setId("edge_1");
        edge1.setSourceNodeId("node_trigger_1");
        edge1.setTargetNodeId("node_gee_load_2");
        edge1.setLabel("start");
        edges.add(edge1);
        
        var edge2 = new com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge();
        edge2.setId("edge_2");
        edge2.setSourceNodeId("node_gee_load_2");
        edge2.setTargetNodeId("node_ndvi_3");
        edge2.setLabel("process");
        edges.add(edge2);
        
        var edge3 = new com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge();
        edge3.setId("edge_3");
        edge3.setSourceNodeId("node_ndvi_3");
        edge3.setTargetNodeId("node_storage_4");
        edge3.setLabel("save");
        edges.add(edge3);
        
        template.setEdges(edges);
        
        return template;
    }
    
    private WorkflowResponse createSimpleTwoTaskTemplate() {
        WorkflowResponse template = new WorkflowResponse();
        template.setId("template_simple_test");
        template.setName("Simple Test Workflow");
        template.setDescription("Basic two-task workflow for testing");
        template.setProjectId("template_project");
        template.setVersion("1.0");
        template.setStatus("TEMPLATE");
        template.setCreatedBy("system");
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        template.setTimeoutSeconds(1800);
        template.setTags(new String[]{"test", "basic", "template"});
        
        List<com.enit.satellite_platform.modules.workflow.entities.WorkflowNode> nodes = new java.util.ArrayList<>();
        
        // Node 1: Trigger
        var triggerNode = new com.enit.satellite_platform.modules.workflow.entities.WorkflowNode();
        triggerNode.setId("node1");
        triggerNode.setType("TRIGGER");
        triggerNode.setName("Start");
        triggerNode.setDescription("Start the workflow");
        triggerNode.setTaskReferenceName("start_trigger");
        var triggerPos = new com.enit.satellite_platform.modules.workflow.entities.NodePosition();
        triggerPos.setX(100.0); triggerPos.setY(100.0);
        triggerNode.setPosition(triggerPos);
        var triggerConfig = new com.enit.satellite_platform.modules.workflow.entities.NodeConfiguration();
        triggerConfig.setTaskType("SIMPLE");
        triggerConfig.setTaskName("workflow_trigger");
        triggerConfig.setTimeoutSeconds(60);
        triggerNode.setConfiguration(triggerConfig);
        triggerNode.setInputParameters(new java.util.HashMap<>());
        nodes.add(triggerNode);
        
        // Node 2: Load Image
        var loadNode = new com.enit.satellite_platform.modules.workflow.entities.WorkflowNode();
        loadNode.setId("node2");
        loadNode.setType("TASK");
        loadNode.setName("Load Image");
        loadNode.setDescription("Load a satellite image");
        loadNode.setTaskReferenceName("load_image_ref");
        var loadPos = new com.enit.satellite_platform.modules.workflow.entities.NodePosition();
        loadPos.setX(300.0); loadPos.setY(100.0);
        loadNode.setPosition(loadPos);
        var loadConfig = new com.enit.satellite_platform.modules.workflow.entities.NodeConfiguration();
        loadConfig.setTaskType("SIMPLE");
        loadConfig.setTaskName("load_image");
        loadConfig.setTimeoutSeconds(300);
        var loadRetry = new com.enit.satellite_platform.modules.workflow.entities.RetryConfig();
        loadRetry.setRetryCount(2);
        loadRetry.setRetryDelaySeconds(10);
        loadRetry.setRetryLogic("FIXED");
        loadConfig.setRetryConfig(loadRetry);
        loadNode.setConfiguration(loadConfig);
        var loadParams = new java.util.HashMap<String, Object>();
        loadParams.put("imageId", "${workflow.input.imageId}");
        loadParams.put("projectId", "${workflow.input.projectId}");
        loadNode.setInputParameters(loadParams);
        nodes.add(loadNode);
        
        template.setNodes(nodes);
        
        List<com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge> edges = new java.util.ArrayList<>();
        var edge1 = new com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge();
        edge1.setId("edge1");
        edge1.setSourceNodeId("node1");
        edge1.setTargetNodeId("node2");
        edge1.setLabel("next");
        edges.add(edge1);
        
        template.setEdges(edges);
        
        return template;
    }
    
    private WorkflowResponse createFullPipelineTemplate() {
        WorkflowResponse template = new WorkflowResponse();
        template.setId("template_full_pipeline");
        template.setName("Complete Data Pipeline");
        template.setDescription("End-to-end pipeline: trigger, fetch data, process, analyze, store");
        template.setProjectId("template_project");
        template.setVersion("1.0");
        template.setStatus("TEMPLATE");
        template.setCreatedBy("system");
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        template.setTimeoutSeconds(7200);
        template.setTags(new String[]{"pipeline", "complete", "production", "template"});
        
        List<com.enit.satellite_platform.modules.workflow.entities.WorkflowNode> nodes = new java.util.ArrayList<>();
        
        // Node 1: Trigger
        var node1 = new com.enit.satellite_platform.modules.workflow.entities.WorkflowNode();
        node1.setId("trigger_node");
        node1.setType("TRIGGER");
        node1.setName("Pipeline Start");
        node1.setDescription("Initiate data pipeline");
        node1.setTaskReferenceName("pipeline_trigger");
        var pos1 = new com.enit.satellite_platform.modules.workflow.entities.NodePosition();
        pos1.setX(100.0); pos1.setY(100.0);
        node1.setPosition(pos1);
        var config1 = new com.enit.satellite_platform.modules.workflow.entities.NodeConfiguration();
        config1.setTaskType("SIMPLE");
        config1.setTaskName("workflow_trigger");
        config1.setTimeoutSeconds(60);
        node1.setConfiguration(config1);
        node1.setInputParameters(new java.util.HashMap<>());
        nodes.add(node1);
        
        // Node 2: Fetch Data
        var node2 = new com.enit.satellite_platform.modules.workflow.entities.WorkflowNode();
        node2.setId("fetch_node");
        node2.setType("TASK");
        node2.setName("Fetch Data");
        node2.setDescription("Retrieve satellite data");
        node2.setTaskReferenceName("fetch_data");
        var pos2 = new com.enit.satellite_platform.modules.workflow.entities.NodePosition();
        pos2.setX(250.0); pos2.setY(100.0);
        node2.setPosition(pos2);
        var config2 = new com.enit.satellite_platform.modules.workflow.entities.NodeConfiguration();
        config2.setTaskType("SIMPLE");
        config2.setTaskName("load_image");
        config2.setTimeoutSeconds(900);
        node2.setConfiguration(config2);
        var params2 = new java.util.HashMap<String, Object>();
        params2.put("imageId", "${workflow.input.imageId}");
        params2.put("projectId", "${workflow.input.projectId}");
        node2.setInputParameters(params2);
        nodes.add(node2);
        
        // Node 3: Process
        var node3 = new com.enit.satellite_platform.modules.workflow.entities.WorkflowNode();
        node3.setId("process_node");
        node3.setType("TASK");
        node3.setName("Process Data");
        node3.setDescription("Apply processing algorithms");
        node3.setTaskReferenceName("process_data");
        var pos3 = new com.enit.satellite_platform.modules.workflow.entities.NodePosition();
        pos3.setX(400.0); pos3.setY(100.0);
        node3.setPosition(pos3);
        var config3 = new com.enit.satellite_platform.modules.workflow.entities.NodeConfiguration();
        config3.setTaskType("SIMPLE");
        config3.setTaskName("ndvi_processing");
        config3.setTimeoutSeconds(900);
        node3.setConfiguration(config3);
        var params3 = new java.util.HashMap<String, Object>();
        params3.put("data", "${fetch_data.output}");
        node3.setInputParameters(params3);
        nodes.add(node3);
        
        // Node 4: Store
        var node4 = new com.enit.satellite_platform.modules.workflow.entities.WorkflowNode();
        node4.setId("store_node");
        node4.setType("TASK");
        node4.setName("Store Results");
        node4.setDescription("Persist results to database");
        node4.setTaskReferenceName("store_data");
        var pos4 = new com.enit.satellite_platform.modules.workflow.entities.NodePosition();
        pos4.setX(550.0); pos4.setY(100.0);
        node4.setPosition(pos4);
        var config4 = new com.enit.satellite_platform.modules.workflow.entities.NodeConfiguration();
        config4.setTaskType("SIMPLE");
        config4.setTaskName("storage_task");
        config4.setTimeoutSeconds(600);
        node4.setConfiguration(config4);
        var params4 = new java.util.HashMap<String, Object>();
        params4.put("results", "${process_data.output}");
        params4.put("projectId", "${workflow.input.projectId}");
        node4.setInputParameters(params4);
        nodes.add(node4);
        
        template.setNodes(nodes);
        
        // Create edges
        List<com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge> edges = new java.util.ArrayList<>();
        edges.add(createEdge("e1", "trigger_node", "fetch_node", "start"));
        edges.add(createEdge("e2", "fetch_node", "process_node", "process"));
        edges.add(createEdge("e3", "process_node", "store_node", "save"));
        
        template.setEdges(edges);
        
        return template;
    }
    
    private com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge createEdge(
            String id, String source, String target, String label) {
        var edge = new com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge();
        edge.setId(id);
        edge.setSourceNodeId(source);
        edge.setTargetNodeId(target);
        edge.setLabel(label);
        return edge;
    }
    
    /**
     * Update workflow
     */
    public WorkflowResponse updateWorkflow(String id, UpdateWorkflowRequest request) {
        log.info("Updating workflow: {}", id);
        
        WorkflowDefinition workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found with id: " + id));
        
        if (request.getName() != null) {
            workflow.setName(request.getName());
        }
        if (request.getDescription() != null) {
            workflow.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            workflow.setStatus(request.getStatus());
        }
        if (request.getNodes() != null) {
            workflow.setNodes(request.getNodes());
            log.info("Updating workflow nodes: {} nodes", request.getNodes().size());
        }
        if (request.getEdges() != null) {
            workflow.setEdges(request.getEdges());
            log.info("Updating workflow edges: {} edges", request.getEdges().size());
        }
        
        workflow.setUpdatedAt(LocalDateTime.now());
        
        WorkflowDefinition updated = workflowRepository.save(workflow);
        log.info("Workflow updated successfully: {}", id);
        
        return toResponse(updated);
    }
    
    /**
     * Delete workflow
     */
    public void deleteWorkflow(String id) {
        log.info("Deleting workflow: {}", id);
        
        if (!workflowRepository.existsById(id)) {
            throw new RuntimeException("Workflow not found with id: " + id);
        }
        
        workflowRepository.deleteById(id);
        log.info("Workflow deleted successfully: {}", id);
    }
    
    /**
     * Publish workflow (change status to PUBLISHED)
     */
    public WorkflowResponse publishWorkflow(String id) {
        log.info("Publishing workflow: {}", id);
        
        WorkflowDefinition workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found with id: " + id));
        
        workflow.setStatus("PUBLISHED");
        workflow.setUpdatedAt(LocalDateTime.now());
        
        WorkflowDefinition published = workflowRepository.save(workflow);
        log.info("Workflow published successfully: {}", id);
        
        return toResponse(published);
    }
    
    /**
     * Convert entity to response DTO
     */
    private WorkflowResponse toResponse(WorkflowDefinition workflow) {
        WorkflowResponse response = new WorkflowResponse();
        response.setId(workflow.getId());
        response.setName(workflow.getName());
        response.setDescription(workflow.getDescription());
        response.setProjectId(workflow.getProjectId());
        response.setVersion(workflow.getVersion());
        response.setStatus(workflow.getStatus());
        response.setCreatedBy(workflow.getCreatedBy());
        response.setCreatedAt(workflow.getCreatedAt());
        response.setUpdatedAt(workflow.getUpdatedAt());
        response.setNodes(workflow.getNodes());
        response.setEdges(workflow.getEdges());
        
        if (workflow.getMetadata() != null) {
            response.setTimeoutSeconds(workflow.getMetadata().getTimeoutSeconds());
            response.setTags(workflow.getMetadata().getTags());
        }
        
        return response;
    }
}

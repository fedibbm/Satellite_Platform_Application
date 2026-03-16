package com.enit.satellite_platform.modules.workflow.execution.nodes;

import com.enit.satellite_platform.modules.project_management.dto.ProjectDto;
import com.enit.satellite_platform.modules.project_management.services.ProjectService;
import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.gee.service.GeeService;
import com.enit.satellite_platform.modules.resource_management.dto.ProcessingResponse;
import com.enit.satellite_platform.modules.resource_management.dto.ServiceRequest;
import com.enit.satellite_platform.modules.resource_management.image_management.dto.ImageDTO;
import com.enit.satellite_platform.modules.resource_management.image_management.services.ImageService;
import com.enit.satellite_platform.modules.workflow.entities.NodeType;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.enit.satellite_platform.modules.workflow.execution.*;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Executor for data input nodes - loads data from project/image services
 */
@Component
public class DataInputNodeExecutor implements NodeExecutor {
    private static final Logger logger = LoggerFactory.getLogger(DataInputNodeExecutor.class);

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ImageService imageService;

    @Autowired(required = false)
    private GeeService geeService;

    @Override
    public NodeType getNodeType() {
        return NodeType.DATA_INPUT;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        logger.info("Executing data input node: {}", node.getId());

        try {
            Map<String, Object> config = context.getResolvedNodeConfig(node);
            
            if (config == null || config.isEmpty()) {
                return NodeExecutionResult.failure("Node configuration is required");
            }

            String dataSource = (String) config.getOrDefault("dataSource", "project");

            switch (dataSource.toLowerCase()) {
                case "project":
                    return NodeExecutionResult.success(loadProjectData(config));

                case "images":
                    return NodeExecutionResult.success(loadImageData(config));

                case "image":
                    return NodeExecutionResult.success(loadSingleImage(config));

                case "gee":
                    return NodeExecutionResult.success(loadGeeData(config, node));

                default:
                    return NodeExecutionResult.failure("Unknown data source: " + dataSource);
            }

        } catch (Exception e) {
            logger.error("Error executing data input node: {}", node.getId(), e);
            return NodeExecutionResult.failure("Data input execution failed: " + e.getMessage());
        }
    }

    /**
     * Load data from Google Earth Engine through the GeeService.
     * Expects configuration compatible with GeneralEarthEngineRequest2 in the Python service.
     */
    private Map<String, Object> loadGeeData(Map<String, Object> config, WorkflowNode node) {
        if (geeService == null) {
            logger.error("GeeService bean is not available - cannot execute GEE data input");
            throw new IllegalStateException("GEE service is not configured on the backend");
        }

        String serviceType = (String) config.getOrDefault("serviceType", "get_images");

        ServiceRequest geeRequest = new ServiceRequest();
        geeRequest.setServiceType(serviceType);

        Map<String, Object> parameters = new HashMap<>();

        if (config.containsKey("collection_id")) {
            parameters.put("collection_id", config.get("collection_id"));
        }
        if (config.containsKey("image_id")) {
            parameters.put("image_id", config.get("image_id"));
        }
        if (config.containsKey("region")) {
            parameters.put("region", config.get("region"));
        }
        if (config.containsKey("start_date")) {
            parameters.put("start_date", config.get("start_date"));
        }
        if (config.containsKey("end_date")) {
            parameters.put("end_date", config.get("end_date"));
        }
        if (config.containsKey("max_cloud_cover")) {
            parameters.put("max_cloud_cover", config.get("max_cloud_cover"));
        }
        if (config.containsKey("scale")) {
            parameters.put("scale", config.get("scale"));
        }
        if (config.containsKey("bands")) {
            parameters.put("bands", config.get("bands"));
        }

        geeRequest.setParameters(parameters);

        logger.info("Calling GeeService for node {} with serviceType: {}", node.getId(), serviceType);
        ProcessingResponse response = geeService.processGeeRequest(geeRequest);

        if (response == null) {
            throw new RuntimeException("GEE service returned null response");
        }

        if ("error".equalsIgnoreCase(response.getStatus())) {
            logger.error("GEE service returned error for node {}: {}", node.getId(), response.getMessage());
            throw new RuntimeException("GEE service error: " + response.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", response.getStatus());
        result.put("message", response.getMessage());
        result.put("data", response.getData());
        result.put("type", response.getType());
        result.put("imageId", response.getImageId());

        return result;
    }

    private Map<String, Object> loadProjectData(Map<String, Object> config) {
        String projectId = (String) config.get("projectId");
        
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required");
        }

        try {
            ObjectId projectObjectId = new ObjectId(projectId);
            ProjectDto project = projectService.getProject(projectObjectId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("dataSource", "project");
            result.put("projectId", project.getId().toString());
            result.put("projectName", project.getProjectName());
            result.put("description", project.getDescription());
            result.put("createdAt", project.getCreatedAt());
            result.put("status", "success");
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error loading project data", e);
            throw new RuntimeException("Failed to load project: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> loadImageData(Map<String, Object> config) {
        String projectId = (String) config.get("projectId");
        
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required for loading images");
        }

        try {
            ObjectId projectObjectId = new ObjectId(projectId);
            
            // Get first page of images (can be enhanced to support pagination)
            Page<ImageDTO> imagesPage = imageService.getImagesByProject(
                projectObjectId, 
                PageRequest.of(0, 100)
            );
            
            List<Map<String, Object>> imageList = imagesPage.getContent().stream()
                .map(img -> {
                    Map<String, Object> imageData = new HashMap<>();
                    imageData.put("imageId", img.getImageId());
                    imageData.put("imageName", img.getImageName());
                    imageData.put("fileSize", img.getFileSize());
                    imageData.put("storageIdentifier", img.getStorageIdentifier());
                    imageData.put("storageType", img.getStorageType());
                    return imageData;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("dataSource", "images");
            result.put("projectId", projectId);
            result.put("imageCount", imageList.size());
            result.put("images", imageList);
            result.put("totalPages", imagesPage.getTotalPages());
            result.put("totalElements", imagesPage.getTotalElements());
            result.put("status", "success");
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error loading image data", e);
            throw new RuntimeException("Failed to load images: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> loadSingleImage(Map<String, Object> config) {
        String imageId = (String) config.get("imageId");
        
        if (imageId == null) {
            throw new IllegalArgumentException("Image ID is required");
        }

        try {
            ImageDTO image = imageService.getImageById(imageId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("dataSource", "image");
            result.put("imageId", image.getImageId());
            result.put("imageName", image.getImageName());
            result.put("fileSize", image.getFileSize());
            result.put("storageIdentifier", image.getStorageIdentifier());
            result.put("storageType", image.getStorageType());
            result.put("status", "success");
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error loading single image", e);
            throw new RuntimeException("Failed to load image: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validate(WorkflowNode node) {
        Map<String, Object> config = node.getData().getConfig();
        
        if (config == null || config.isEmpty()) {
            logger.warn("Node {} has no configuration", node.getId());
            return false;
        }

        String dataSource = (String) config.getOrDefault("dataSource", "project");

        switch (dataSource.toLowerCase()) {
            case "project":
                return config.containsKey("projectId");

            case "images":
                return config.containsKey("projectId");

            case "image":
                return config.containsKey("imageId");

            case "gee":
                boolean hasRegion = config.containsKey("region");
                boolean hasCollectionOrImage = config.containsKey("collection_id") || config.containsKey("image_id");
                if (!hasRegion || !hasCollectionOrImage) {
                    logger.warn("GEE data-input node {} is missing required 'region' or 'collection_id'/'image_id'", node.getId());
                }
                return hasRegion && hasCollectionOrImage;

            default:
                return false;
        }
    }

    @Override
    public NodeMetadata getMetadata() {
        Map<String, String> schema = new HashMap<>();
        schema.put("dataSource", "String: project, images, or image");
        schema.put("projectId", "String: ObjectId of the project (for project/images sources)");
        schema.put("imageId", "String: ID of the image (for image source)");

        return new NodeMetadata(
            "Data Input",
            "Load data from project or image management services",
            "Input",
            schema,
            java.util.List.of(),
            java.util.List.of("projectData", "imageData")
        );
    }
}

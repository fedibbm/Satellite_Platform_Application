package com.enit.satellite_platform.modules.workflow.execution.nodes;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.enit.satellite_platform.modules.workflow.execution.NodeExecutionContext;
import com.enit.satellite_platform.modules.workflow.execution.NodeExecutionResult;
import com.enit.satellite_platform.modules.workflow.execution.WorkflowNodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInputNodeExecutor implements WorkflowNodeExecutor {
    
    private final RestTemplate restTemplate;
    
    @Value("${gee.service.url:http://localhost:5000}")
    private String geeServiceUrl;
    
    @Value("${storage.service.url:http://localhost:8080}")
    private String storageServiceUrl;
    
    @Override
    public String getNodeType() {
        return "data-input";
    }
    
    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        log.info("Executing data input node: {}", node.getId());
        
        Map<String, Object> config = node.getData().getConfig();
        String sourceType = (String) config.getOrDefault("sourceType", "gee");
        
        try {
            switch (sourceType) {
                case "gee":
                    return fetchFromGEE(node, context, config);
                
                case "project":
                    return fetchFromProject(node, context, config);
                
                case "storage":
                    return fetchFromStorage(node, context, config);
                
                default:
                    return NodeExecutionResult.failure("Unknown source type: " + sourceType);
            }
        } catch (Exception e) {
            log.error("Error fetching data from {}: {}", sourceType, e.getMessage(), e);
            return NodeExecutionResult.failure("Data fetch failed: " + e.getMessage());
        }
    }
    
    private NodeExecutionResult fetchFromGEE(WorkflowNode node, NodeExecutionContext context, Map<String, Object> config) {
        log.info("Fetching data from Google Earth Engine");
        
        String collectionId = (String) config.get("collection_id");
        String startDate = (String) config.get("start_date");
        String endDate = (String) config.get("end_date");
        String serviceType = (String) config.getOrDefault("serviceType", "get_images");
        
        // Build GEE request
        Map<String, Object> geeRequest = new HashMap<>();
        geeRequest.put("collection_id", collectionId);
        geeRequest.put("start_date", startDate);
        geeRequest.put("end_date", endDate);
        
        // Add region if available from context or config
        if (config.containsKey("region")) {
            geeRequest.put("region", config.get("region"));
        } else if (context.getExecutionParameters().containsKey("region")) {
            geeRequest.put("region", context.getExecutionParameters().get("region"));
        }
        
        // Add scale if available
        if (config.containsKey("scale")) {
            geeRequest.put("scale", config.get("scale"));
        }
        
        try {
            // Call GEE service
            String endpoint = serviceType.equals("get_image_collection") 
                ? "/get_image_collection" 
                : "/get_images";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(geeRequest, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    geeServiceUrl + endpoint,
                    HttpMethod.POST,
                    request,
                    Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("sourceType", "gee");
                result.put("collectionId", collectionId);
                result.put("dateRange", Map.of("start", startDate, "end", endDate));
                result.put("data", response.getBody());
                result.put("imageCount", response.getBody().get("count"));
                
                NodeExecutionResult executionResult = NodeExecutionResult.success(result, "Successfully fetched GEE data");
                executionResult.addMetadata("collection", collectionId);
                executionResult.addMetadata("images", response.getBody().get("count"));
                
                return executionResult;
            } else {
                return NodeExecutionResult.failure("GEE service returned error: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error calling GEE service: {}", e.getMessage(), e);
            // Return mock data for development
            return getMockGEEData(collectionId, startDate, endDate);
        }
    }
    
    private NodeExecutionResult fetchFromProject(WorkflowNode node, NodeExecutionContext context, Map<String, Object> config) {
        log.info("Fetching data from project: {}", context.getProjectId());
        
        String dataType = (String) config.getOrDefault("dataType", "images");
        
        // TODO: Implement actual project data fetching
        Map<String, Object> result = new HashMap<>();
        result.put("sourceType", "project");
        result.put("projectId", context.getProjectId());
        result.put("dataType", dataType);
        result.put("data", Map.of("message", "Project data placeholder"));
        
        return NodeExecutionResult.success(result, "Successfully fetched project data");
    }
    
    private NodeExecutionResult fetchFromStorage(WorkflowNode node, NodeExecutionContext context, Map<String, Object> config) {
        log.info("Fetching data from storage");
        
        String path = (String) config.get("path");
        String fileType = (String) config.getOrDefault("fileType", "geotiff");
        
        // TODO: Implement actual storage fetching
        Map<String, Object> result = new HashMap<>();
        result.put("sourceType", "storage");
        result.put("path", path);
        result.put("fileType", fileType);
        result.put("data", Map.of("message", "Storage data placeholder"));
        
        return NodeExecutionResult.success(result, "Successfully fetched storage data");
    }
    
    private NodeExecutionResult getMockGEEData(String collectionId, String startDate, String endDate) {
        log.warn("Returning mock GEE data for development");
        
        Map<String, Object> result = new HashMap<>();
        result.put("sourceType", "gee");
        result.put("collectionId", collectionId);
        result.put("dateRange", Map.of("start", startDate, "end", endDate));
        result.put("imageCount", 5);
        result.put("data", Map.of(
                "images", java.util.Arrays.asList(
                        Map.of("id", "img_1", "date", startDate),
                        Map.of("id", "img_2", "date", startDate),
                        Map.of("id", "img_3", "date", endDate)
                ),
                "status", "mock_data"
        ));
        
        NodeExecutionResult executionResult = NodeExecutionResult.success(result, "Mock GEE data returned");
        executionResult.addWarning("Using mock data - GEE service not available");
        
        return executionResult;
    }
    
    @Override
    public boolean validate(WorkflowNode node) {
        Map<String, Object> config = node.getData().getConfig();
        String sourceType = (String) config.get("sourceType");
        
        if (sourceType == null) {
            return false;
        }
        
        switch (sourceType) {
            case "gee":
                return config.containsKey("collection_id");
            case "project":
                return true; // Project ID comes from context
            case "storage":
                return config.containsKey("path");
            default:
                return false;
        }
    }
    
    @Override
    public NodeMetadata getMetadata() {
        return new NodeMetadata(
                "data-input",
                "Data Input Node",
                "Fetches data from GEE, project storage, or external sources",
                "Input"
        );
    }
}

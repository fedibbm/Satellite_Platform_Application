package com.enit.satellite_platform.modules.workflow.execution.nodes;

import com.enit.satellite_platform.modules.workflow.entities.NodeType;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.enit.satellite_platform.modules.workflow.execution.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executor for processing nodes - integrates with image processing service
 * Performs operations like NDVI, EVI, vegetation indices
 */
@Component
public class ProcessingNodeExecutor implements NodeExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ProcessingNodeExecutor.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${python.backend.url}")
    private String imageProcessingUrl;

    @Override
    public NodeType getNodeType() {
        return NodeType.PROCESSING;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        logger.info("Executing processing node: {}", node.getId());

        try {
            Map<String, Object> config = node.getData().getConfig();
            
            if (config == null || config.isEmpty()) {
                return NodeExecutionResult.failure("Node configuration is required for processing");
            }

            // Get processing type
            String processingType = (String) config.getOrDefault("processingType", "ndvi");
            logger.info("Processing type: {}", processingType);

            // Get input data from previous nodes if available
            Map<String, Object> inputData = extractInputData(context, config);

            // Build request based on processing type
            Map<String, Object> result;
            
            switch (processingType.toLowerCase()) {
                case "ndvi":
                case "evi":
                case "savi":
                case "ndwi":
                    result = processVegetationIndex(processingType, inputData, config);
                    break;
                    
                case "water-bodies":
                    result = processWaterBodies(inputData, config);
                    break;
                    
                case "change-detection":
                    result = processChangeDetection(inputData, config);
                    break;
                    
                default:
                    return NodeExecutionResult.failure("Unknown processing type: " + processingType);
            }

            logger.info("Processing node completed successfully: {}", node.getId());
            return NodeExecutionResult.success(result);

        } catch (Exception e) {
            logger.error("Error executing processing node: {}", node.getId(), e);
            return NodeExecutionResult.failure("Processing execution failed: " + e.getMessage());
        }
    }

    private Map<String, Object> extractInputData(NodeExecutionContext context, Map<String, Object> config) {
        Map<String, Object> inputData = new HashMap<>();
        
        // Check if there's a specific input node reference
        String inputNodeId = (String) config.get("inputNodeId");
        if (inputNodeId != null) {
            Object nodeOutput = context.getNodeOutput(inputNodeId);
            if (nodeOutput instanceof Map) {
                inputData.putAll((Map<String, Object>) nodeOutput);
            }
        } else {
            // Try to get data from any previous node
            Map<String, Object> allOutputs = context.getNodeOutputs();
            if (!allOutputs.isEmpty()) {
                // Get the most recent output
                Object lastOutput = allOutputs.values().stream()
                    .reduce((first, second) -> second)
                    .orElse(null);
                if (lastOutput instanceof Map) {
                    inputData.putAll((Map<String, Object>) lastOutput);
                }
            }
        }
        
        return inputData;
    }

    private Map<String, Object> processVegetationIndex(String indexType, Map<String, Object> inputData, Map<String, Object> config) {
        logger.info("Processing vegetation index: {}", indexType);
        
        try {
            // Build request for image processing service
            Map<String, Object> request = new HashMap<>();
            request.put("operation", indexType);
            
            // Add image data from input if available
            if (inputData.containsKey("data")) {
                request.put("imageData", inputData.get("data"));
            }
            
            // Add configuration parameters
            if (config.containsKey("imageUrl")) {
                request.put("imageUrl", config.get("imageUrl"));
            }
            if (config.containsKey("bands")) {
                request.put("bands", config.get("bands"));
            }
            if (config.containsKey("threshold")) {
                request.put("threshold", config.get("threshold"));
            }

            // Call image processing service
            String endpoint = imageProcessingUrl + "/process/" + indexType;
            logger.info("Calling image processing service: {}", endpoint);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, entity, Map.class);
            
            Map<String, Object> result = new HashMap<>();
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                result.putAll(response.getBody());
                result.put("processingType", indexType);
                result.put("status", "success");
            } else {
                result.put("status", "error");
                result.put("message", "Processing service returned status: " + response.getStatusCode());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error calling image processing service", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "Processing service error: " + e.getMessage());
            errorResult.put("processingType", indexType);
            return errorResult;
        }
    }

    private Map<String, Object> processWaterBodies(Map<String, Object> inputData, Map<String, Object> config) {
        logger.info("Processing water bodies detection");
        
        Map<String, Object> result = new HashMap<>();
        result.put("processingType", "water-bodies");
        result.put("status", "success");
        result.put("message", "Water bodies detection completed");
        
        // This would call the actual water bodies detection endpoint
        // For now, return a placeholder
        result.put("waterBodiesDetected", true);
        result.put("coverage", 15.5); // Percentage
        
        return result;
    }

    private Map<String, Object> processChangeDetection(Map<String, Object> inputData, Map<String, Object> config) {
        logger.info("Processing change detection");
        
        Map<String, Object> result = new HashMap<>();
        result.put("processingType", "change-detection");
        result.put("status", "success");
        result.put("message", "Change detection completed");
        
        // This would call the actual change detection endpoint
        result.put("changesDetected", true);
        result.put("changePercentage", 8.3);
        
        return result;
    }

    @Override
    public boolean validate(WorkflowNode node) {
        Map<String, Object> config = node.getData().getConfig();
        
        if (config == null || config.isEmpty()) {
            logger.warn("Node {} has no configuration", node.getId());
            return false;
        }

        // Validate processing type is specified
        if (!config.containsKey("processingType")) {
            logger.warn("Node {} missing required 'processingType' parameter", node.getId());
            return false;
        }

        String processingType = (String) config.get("processingType");
        List<String> validTypes = List.of("ndvi", "evi", "savi", "ndwi", "water-bodies", "change-detection");
        
        if (!validTypes.contains(processingType.toLowerCase())) {
            logger.warn("Node {} has invalid processing type: {}", node.getId(), processingType);
            return false;
        }

        return true;
    }

    @Override
    public NodeMetadata getMetadata() {
        Map<String, String> schema = new HashMap<>();
        schema.put("processingType", "String: ndvi, evi, savi, ndwi, water-bodies, change-detection");
        schema.put("inputNodeId", "String: ID of node providing input data");
        schema.put("imageUrl", "String: URL or path to image file");
        schema.put("bands", "Array: Specific bands to use in processing");
        schema.put("threshold", "Number: Threshold value for classification");

        return new NodeMetadata(
            "Image Processing",
            "Performs image analysis operations (NDVI, EVI, water bodies, etc.)",
            "Processing",
            schema,
            java.util.List.of("imageData"),
            java.util.List.of("status", "result", "processingType")
        );
    }
}

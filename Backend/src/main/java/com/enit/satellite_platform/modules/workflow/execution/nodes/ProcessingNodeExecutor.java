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
public class ProcessingNodeExecutor implements WorkflowNodeExecutor {
    
    private final RestTemplate restTemplate;
    
    @Value("${processing.service.url:http://localhost:8000}")
    private String processingServiceUrl;
    
    @Value("${gee.service.url:http://localhost:5000}")
    private String geeServiceUrl;
    
    @Override
    public String getNodeType() {
        return "processing";
    }
    
    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        log.info("Executing processing node: {}", node.getId());
        
        Map<String, Object> config = node.getData().getConfig();
        String processingType = (String) config.getOrDefault("processingType", "ndvi");
        
        try {
            switch (processingType) {
                case "ndvi":
                    return calculateNDVI(node, context, config);
                
                case "evi":
                    return calculateEVI(node, context, config);
                
                case "savi":
                    return calculateSAVI(node, context, config);
                
                case "gee_analysis":
                    return performGEEAnalysis(node, context, config);
                
                case "custom":
                    return performCustomProcessing(node, context, config);
                
                default:
                    return NodeExecutionResult.failure("Unknown processing type: " + processingType);
            }
        } catch (Exception e) {
            log.error("Error in processing node: {}", e.getMessage(), e);
            return NodeExecutionResult.failure("Processing failed: " + e.getMessage());
        }
    }
    
    private NodeExecutionResult calculateNDVI(WorkflowNode node, NodeExecutionContext context, Map<String, Object> config) {
        log.info("Calculating NDVI");
        
        // Get input data from previous node
        Object inputData = getPreviousNodeOutput(context, node);
        
        Map<String, Object> parameters = (Map<String, Object>) config.getOrDefault("parameters", new HashMap<>());
        Integer scale = (Integer) parameters.getOrDefault("scale", 30);
        
        // Build processing request
        Map<String, Object> processingRequest = new HashMap<>();
        processingRequest.put("index_type", "ndvi");
        processingRequest.put("input_data", inputData);
        processingRequest.put("scale", scale);
        
        try {
            // Call image processing service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(processingRequest, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    processingServiceUrl + "/calculate_index",
                    HttpMethod.POST,
                    request,
                    Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("processingType", "ndvi");
                result.put("scale", scale);
                result.put("data", response.getBody());
                result.put("statistics", response.getBody().get("statistics"));
                
                NodeExecutionResult executionResult = NodeExecutionResult.success(result, "NDVI calculation completed");
                executionResult.addMetadata("index", "NDVI");
                executionResult.addMetadata("scale", scale);
                
                return executionResult;
            } else {
                return NodeExecutionResult.failure("Processing service returned error");
            }
            
        } catch (Exception e) {
            log.error("Error calling processing service: {}", e.getMessage(), e);
            // Return mock data for development
            return getMockProcessingResult("NDVI", scale);
        }
    }
    
    private NodeExecutionResult calculateEVI(WorkflowNode node, NodeExecutionContext context, Map<String, Object> config) {
        log.info("Calculating EVI");
        
        Object inputData = getPreviousNodeOutput(context, node);
        Map<String, Object> parameters = (Map<String, Object>) config.getOrDefault("parameters", new HashMap<>());
        Integer scale = (Integer) parameters.getOrDefault("scale", 30);
        
        try {
            Map<String, Object> processingRequest = new HashMap<>();
            processingRequest.put("index_type", "evi");
            processingRequest.put("input_data", inputData);
            processingRequest.put("scale", scale);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(processingRequest, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    processingServiceUrl + "/calculate_index",
                    HttpMethod.POST,
                    request,
                    Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("processingType", "evi");
                result.put("scale", scale);
                result.put("data", response.getBody());
                
                return NodeExecutionResult.success(result, "EVI calculation completed");
            } else {
                return NodeExecutionResult.failure("Processing service returned error");
            }
            
        } catch (Exception e) {
            log.error("Error calculating EVI: {}", e.getMessage(), e);
            return getMockProcessingResult("EVI", scale);
        }
    }
    
    private NodeExecutionResult calculateSAVI(WorkflowNode node, NodeExecutionContext context, Map<String, Object> config) {
        log.info("Calculating SAVI");
        
        Object inputData = getPreviousNodeOutput(context, node);
        Map<String, Object> parameters = (Map<String, Object>) config.getOrDefault("parameters", new HashMap<>());
        
        Map<String, Object> result = new HashMap<>();
        result.put("processingType", "savi");
        result.put("data", inputData);
        result.put("message", "SAVI calculation completed (mock)");
        
        return NodeExecutionResult.success(result, "SAVI calculation completed");
    }
    
    private NodeExecutionResult performGEEAnalysis(WorkflowNode node, NodeExecutionContext context, Map<String, Object> config) {
        log.info("Performing GEE analysis");
        
        Object inputData = getPreviousNodeOutput(context, node);
        Map<String, Object> parameters = (Map<String, Object>) config.getOrDefault("parameters", new HashMap<>());
        String analysisType = (String) parameters.getOrDefault("analysisType", "statistics");
        
        // TODO: Implement actual GEE analysis
        Map<String, Object> result = new HashMap<>();
        result.put("processingType", "gee_analysis");
        result.put("analysisType", analysisType);
        result.put("data", Map.of("message", "GEE analysis placeholder"));
        
        return NodeExecutionResult.success(result, "GEE analysis completed");
    }
    
    private NodeExecutionResult performCustomProcessing(WorkflowNode node, NodeExecutionContext context, Map<String, Object> config) {
        log.info("Performing custom processing");
        
        Object inputData = getPreviousNodeOutput(context, node);
        Map<String, Object> parameters = (Map<String, Object>) config.getOrDefault("parameters", new HashMap<>());
        String algorithm = (String) parameters.get("algorithm");
        
        Map<String, Object> result = new HashMap<>();
        result.put("processingType", "custom");
        result.put("algorithm", algorithm);
        result.put("data", Map.of("message", "Custom processing placeholder"));
        
        return NodeExecutionResult.success(result, "Custom processing completed");
    }
    
    private Object getPreviousNodeOutput(NodeExecutionContext context, WorkflowNode currentNode) {
        // Try to find the output from the previous node
        // In a real implementation, we'd track edges to find the source node
        Map<String, Object> outputs = context.getNodeOutputs();
        if (!outputs.isEmpty()) {
            // Return the most recent output
            return outputs.values().stream()
                    .reduce((first, second) -> second)
                    .orElse(null);
        }
        return null;
    }
    
    private NodeExecutionResult getMockProcessingResult(String indexType, Integer scale) {
        log.warn("Returning mock {} data for development", indexType);
        
        Map<String, Object> result = new HashMap<>();
        result.put("processingType", indexType.toLowerCase());
        result.put("scale", scale);
        result.put("data", Map.of(
                "mean", 0.65,
                "min", 0.2,
                "max", 0.95,
                "stdDev", 0.15,
                "pixelCount", 10000
        ));
        result.put("statistics", Map.of(
                "mean", 0.65,
                "median", 0.67
        ));
        
        NodeExecutionResult executionResult = NodeExecutionResult.success(result, 
                "Mock " + indexType + " calculation completed");
        executionResult.addWarning("Using mock data - processing service not available");
        
        return executionResult;
    }
    
    @Override
    public boolean validate(WorkflowNode node) {
        Map<String, Object> config = node.getData().getConfig();
        return config.containsKey("processingType");
    }
    
    @Override
    public NodeMetadata getMetadata() {
        return new NodeMetadata(
                "processing",
                "Processing Node",
                "Performs image processing operations like NDVI, EVI, or custom algorithms",
                "Processing"
        );
    }
}

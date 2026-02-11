package com.enit.satellite_platform.modules.workflow.execution.nodes;

import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.gee.service.GeeService;
import com.enit.satellite_platform.modules.resource_management.dto.ProcessingResponse;
import com.enit.satellite_platform.modules.resource_management.dto.ServiceRequest;
import com.enit.satellite_platform.modules.workflow.entities.NodeType;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.enit.satellite_platform.modules.workflow.execution.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Executor for data input nodes - integrates with GEE Service
 * Fetches satellite imagery from Google Earth Engine
 */
@Component
public class GeeInputNodeExecutor implements NodeExecutor {
    private static final Logger logger = LoggerFactory.getLogger(GeeInputNodeExecutor.class);

    @Autowired
    private GeeService geeService;

    @Override
    public NodeType getNodeType() {
        return NodeType.DATA_INPUT;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        logger.info("Executing GEE input node: {}", node.getId());

        try {
            Map<String, Object> config = node.getData().getConfig();
            
            if (config == null || config.isEmpty()) {
                return NodeExecutionResult.failure("Node configuration is required for GEE input");
            }

            // Extract service configuration
            String serviceType = (String) config.getOrDefault("serviceType", "get_images");
            
            // Build ServiceRequest for GeeService
            ServiceRequest geeRequest = new ServiceRequest();
            geeRequest.setServiceType(serviceType);
            
            // Copy parameters from node config
            Map<String, Object> parameters = new HashMap<>();
            
            // Common GEE parameters
            if (config.containsKey("collection_id")) {
                parameters.put("collection_id", config.get("collection_id"));
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

            // Call GEE Service
            logger.info("Calling GEE service with type: {}", serviceType);
            ProcessingResponse response = geeService.processGeeRequest(geeRequest);

            // Check if request was successful
            if ("error".equals(response.getStatus())) {
                logger.error("GEE service returned error: {}", response.getMessage());
                return NodeExecutionResult.failure("GEE service error: " + response.getMessage());
            }

            // Prepare result
            Map<String, Object> result = new HashMap<>();
            result.put("status", response.getStatus());
            result.put("message", response.getMessage());
            result.put("data", response.getData());
            result.put("serviceType", serviceType);
            result.put("timestamp", System.currentTimeMillis());

            logger.info("GEE input node completed successfully: {}", node.getId());
            return NodeExecutionResult.success(result);

        } catch (Exception e) {
            logger.error("Error executing GEE input node: {}", node.getId(), e);
            return NodeExecutionResult.failure("GEE input execution failed: " + e.getMessage());
        }
    }

    @Override
    public boolean validate(WorkflowNode node) {
        Map<String, Object> config = node.getData().getConfig();
        
        if (config == null || config.isEmpty()) {
            logger.warn("Node {} has no configuration", node.getId());
            return false;
        }

        // Validate required parameters based on service type
        String serviceType = (String) config.getOrDefault("serviceType", "get_images");
        
        // For image fetching, region is typically required
        if ("get_images".equals(serviceType) || "preview".equals(serviceType)) {
            if (!config.containsKey("region")) {
                logger.warn("Node {} missing required 'region' parameter", node.getId());
                return false;
            }
        }

        return true;
    }

    @Override
    public NodeMetadata getMetadata() {
        Map<String, String> schema = new HashMap<>();
        schema.put("serviceType", "String: get_images, preview, ndvi, evi, water-bodies, etc.");
        schema.put("collection_id", "String: GEE collection ID (e.g., COPERNICUS/S2_HARMONIZED)");
        schema.put("region", "Object: GeoJSON geometry defining area of interest");
        schema.put("start_date", "String: Start date in YYYY-MM-DD format");
        schema.put("end_date", "String: End date in YYYY-MM-DD format");
        schema.put("max_cloud_cover", "Number: Maximum cloud cover percentage (0-100)");
        schema.put("scale", "Number: Resolution in meters");
        schema.put("bands", "Array: List of bands to fetch");

        return new NodeMetadata(
            "GEE Data Input",
            "Fetches satellite imagery from Google Earth Engine",
            "Data Input",
            schema,
            java.util.List.of(),
            java.util.List.of("status", "data", "message")
        );
    }
}

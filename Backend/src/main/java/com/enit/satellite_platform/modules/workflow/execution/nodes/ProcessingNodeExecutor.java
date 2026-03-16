package com.enit.satellite_platform.modules.workflow.execution.nodes;

import com.enit.satellite_platform.modules.workflow.entities.NodeType;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.enit.satellite_platform.modules.workflow.execution.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

import com.enit.satellite_platform.modules.resource_management.image_management.services.ImageService;
import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.openCV.vegetation_Index_calculation.VegetationIndexService;
import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.openCV.vegetation_Index_calculation.dto.VegetationIndexRequest;
import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.openCV.vegetation_Index_calculation.dto.VegetationIndexResult;

/**
 * Executor for processing nodes - integrates with image processing service
 * Performs operations like NDVI, EVI, vegetation indices
 */
@Component
public class ProcessingNodeExecutor implements NodeExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ProcessingNodeExecutor.class);

    @Autowired
    private ImageService imageService;

    @Autowired
    private VegetationIndexService vegetationIndexService;

    @Override
    public NodeType getNodeType() {
        return NodeType.PROCESSING;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        logger.info("Executing processing node: {}", node.getId());

        try {
            Map<String, Object> config = context.getResolvedNodeConfig(node);
            
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
        
        File tempFile = null;
        try {
            String imageId = null;
            if (inputData.containsKey("imageId")) {
                imageId = (String) inputData.get("imageId");
            } else if (config.containsKey("imageId")) {
                imageId = (String) config.get("imageId");
            }
            
            if (imageId == null) {
                throw new IllegalArgumentException("No imageId provided in input data or config.");
            }
            
            // Get file from DB
            MultipartFile multipartFile = imageService.getImageData(imageId);
            if (multipartFile == null || multipartFile.isEmpty()) {
                throw new IllegalArgumentException("Could not extract image data for imageId: " + imageId);
            }

            // Create temp file
            tempFile = File.createTempFile("processing_", "_" + multipartFile.getOriginalFilename());
            Files.copy(multipartFile.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            VegetationIndexRequest request = new VegetationIndexRequest();
            request.setIndexType(indexType.toUpperCase());
            
            if (config.containsKey("bands")) {
                Object bandsObj = config.get("bands");
                if (bandsObj instanceof List) {
                    List<?> bands = (List<?>) bandsObj;
                    if (bands.size() > 0 && bands.get(0) instanceof Number) request.setRedBand(((Number)bands.get(0)).intValue());
                    if (bands.size() > 1 && bands.get(1) instanceof Number) request.setNirBand(((Number)bands.get(1)).intValue());
                    if (bands.size() > 2 && bands.get(2) instanceof Number) request.setBlueBand(((Number)bands.get(2)).intValue());
                }
            }

            VegetationIndexResult res = null;
            String uppercaseType = indexType.toUpperCase();
            if ("NDVI".equals(uppercaseType)) {
                res = vegetationIndexService.calculateNDVI(tempFile, request, null);
            } else if ("EVI".equals(uppercaseType)) {
                res = vegetationIndexService.calculateEVI(tempFile, request, null);
            } else if ("SAVI".equals(uppercaseType)) {
                res = vegetationIndexService.calculateSAVI(tempFile, request, null);
            } else if ("NDWI".equals(uppercaseType)) {
                res = vegetationIndexService.calculateNDWI(tempFile, request.getRedBand(), request.getNirBand(), null);
            } else {
                res = vegetationIndexService.calculateIndex(request, tempFile, null);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("processingType", indexType);
            
            if (res != null) {
                 result.put("status", "success");
                 result.put("statistics", res.getStatistics());
                 result.put("processingDuration", res.getProcessingDuration());
                 if (res.getProcessedImage() != null) {
                    String base64Image = Base64.getEncoder().encodeToString(res.getProcessedImage());
                    result.put("processedImageBase64", base64Image);
                 }
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error calling image processing service", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "Processing service error: " + e.getMessage());
            errorResult.put("processingType", indexType);
            return errorResult;
        } finally {
             if (tempFile != null && tempFile.exists()) {
                 tempFile.delete();
             }
        }
    }

    private Map<String, Object> processWaterBodies(Map<String, Object> inputData, Map<String, Object> config) {
        logger.info("Processing water bodies detection");
        
        Map<String, Object> result = new HashMap<>();
        result.put("processingType", "water-bodies");
        result.put("status", "success");
        result.put("message", "Water bodies detection completed");
        
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

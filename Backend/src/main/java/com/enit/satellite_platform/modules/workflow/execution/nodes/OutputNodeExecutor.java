package com.enit.satellite_platform.modules.workflow.execution.nodes;

import com.enit.satellite_platform.modules.workflow.entities.NodeType;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.enit.satellite_platform.modules.workflow.execution.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.enit.satellite_platform.modules.resource_management.image_management.services.ProcessingResultsService;
import com.enit.satellite_platform.modules.resource_management.image_management.dto.resultsSaveRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Executor for output nodes
 * Handles saving results to various destinations
 */
@Component
public class OutputNodeExecutor implements NodeExecutor {
    @Autowired(required = false)
    private ProcessingResultsService resultsService;
    private static final Logger logger = LoggerFactory.getLogger(OutputNodeExecutor.class);

    @Override
    public NodeType getNodeType() {
        return NodeType.OUTPUT;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        logger.info("Executing output node: {}", node.getId());

        try {
            // Retrieve config and interpolate any incoming data parameters
            Map<String, Object> config = context.getResolvedNodeConfig(node);
            String outputType = config != null ? 
                (String) config.getOrDefault("outputType", "project") : "project";

            // Determine if the config explicitly references prior node outputs to save
            Object dataToSave = config != null ? config.get("dataToSave") : null;
            
            // If nothing is explicitly mapped, we might default to ALL node outputs
            if (dataToSave == null) {
                dataToSave = context.getNodeOutputs();
            }


            Map<String, Object> result = new HashMap<>();
            result.put("saved", true);
            result.put("outputType", outputType);
            result.put("location", "/output/" + node.getId());
            
            int savedCount = 0;
            if (resultsService != null && dataToSave instanceof Map) {
                Map<String, Object> nodesData = (Map<String, Object>) dataToSave;
                for (Map.Entry<String, Object> entry : nodesData.entrySet()) {
                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> nodeOutput = (Map<String, Object>) entry.getValue();
                        if (nodeOutput.containsKey("processedImageBase64")) {
                            String base64 = (String) nodeOutput.get("processedImageBase64");
                            String pType = (String) nodeOutput.getOrDefault("processingType", "workflow-output");
                            
                            resultsSaveRequest req = new resultsSaveRequest();
                            req.setProjectId((String) context.getGlobalVariables().getOrDefault("projectId", ""));
                            try {
                                req.setType(com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingType.fromString(pType));
                            } catch (Exception paramE) {
                                req.setType(com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingType.NDVI); // Default fallback
                            }
                            
                            Map<String, Object> dataMap = new HashMap<>();
                            dataMap.put("processedImageBase64", base64);
                            // In real scenario, convert base64 to byte[] and save as file
                            req.setData(dataMap);
                            req.setDate(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                            req.setStatus(com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingStatus.COMPLETED);
                            
                            try {
                                resultsService.save(req);
                                savedCount++;
                            } catch(Exception ex) {
                                logger.error("Failed to save output to ProcessingResultsService", ex);
                            }
                        }
                    }
                }
            }
            result.put("savedItemsCount", savedCount);
            result.put("savedDataSnapshot", savedCount > 0 ? "Saved to Results Service" : "Snapshot taken");


            // In a real implementation, this would:
            // 2. Save to the specified destination (project, storage, etc.)
            // 3. Return the saved location/reference

            logger.info("Output node executed successfully: {}", node.getId());
            return NodeExecutionResult.success(result);

        } catch (Exception e) {
            logger.error("Error executing output node: {}", node.getId(), e);
            return NodeExecutionResult.failure("Output execution failed: " + e.getMessage());
        }
    }

    @Override
    public boolean validate(WorkflowNode node) {
        Map<String, Object> config = node.getData().getConfig();
        if (config == null) {
            return false;
        }
        // Add validation logic here
        return true;
    }

    @Override
    public NodeMetadata getMetadata() {
        Map<String, String> schema = new HashMap<>();
        schema.put("outputType", "String: project, storage, export");
        schema.put("format", "String: geotiff, png, json");

        return new NodeMetadata(
            "Output",
            "Saves workflow results to specified destination",
            "Output",
            schema,
            java.util.List.of("data"),
            java.util.List.of("saved", "location")
        );
    }
}

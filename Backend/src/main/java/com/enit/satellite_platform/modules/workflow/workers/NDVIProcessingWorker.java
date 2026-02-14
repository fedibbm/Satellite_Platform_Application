package com.enit.satellite_platform.modules.workflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

/**
 * Worker for calculate_ndvi tasks.
 * Processes satellite images and calculates NDVI (Normalized Difference Vegetation Index).
 */
@Slf4j
@Component
public class NDVIProcessingWorker extends BaseTaskWorker {

    private final RestTemplate restTemplate;
    
    @Value("${image.processing.url:http://localhost:8000}")
    private String imageProcessingUrl;

    public NDVIProcessingWorker() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String getTaskDefName() {
        return "calculate_ndvi";
    }

    @Override
    protected Map<String, Object> executeTask(Task task) throws Exception {
        log.info("NDVIProcessingWorker: Starting NDVI calculation");
        
        // Get required parameters
        String imageId = (String) getRequiredInput(task, "imageId");
        Object imageData = getRequiredInput(task, "imageData");
        
        // Optional parameters
        String algorithm = (String) getOptionalInput(task, "algorithm", "standard");
        Double threshold = (Double) getOptionalInput(task, "threshold", 0.3);
        
        log.info("NDVIProcessingWorker: ImageId={}, Algorithm={}, Threshold={}", 
            imageId, algorithm, threshold);
        
        try {
            // Call image processing service
            String url = imageProcessingUrl + "/api/process/ndvi";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Prepare request payload
            Map<String, Object> requestBody = createOutput();
            requestBody.put("imageId", imageId);
            requestBody.put("imageData", imageData);
            requestBody.put("algorithm", algorithm);
            requestBody.put("threshold", threshold);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            log.info("NDVIProcessingWorker: Calling image processing service at {}", url);
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                Map.class
            );
            
            Map<String, Object> processingResult = response.getBody();
            if (processingResult == null) {
                throw new RuntimeException("Image processing service returned empty response");
            }
            
            // Create output with NDVI results
            Map<String, Object> output = createOutput();
            output.put("imageId", imageId);
            output.put("ndviData", processingResult.get("ndvi"));
            output.put("ndviUrl", processingResult.get("ndviUrl"));
            output.put("statistics", processingResult.get("statistics"));
            output.put("algorithm", algorithm);
            output.put("threshold", threshold);
            output.put("status", "processed");
            
            log.info("NDVIProcessingWorker: NDVI calculation completed successfully");
            return output;
            
        } catch (Exception e) {
            log.error("NDVIProcessingWorker: Failed to process image: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate NDVI: " + e.getMessage(), e);
        }
    }

    @Override
    protected void validateInput(Task task) throws IllegalArgumentException {
        super.validateInput(task);
        
        String imageId = (String) task.getInputData().get("imageId");
        if (imageId == null || imageId.trim().isEmpty()) {
            throw new IllegalArgumentException("imageId parameter is required");
        }
        
        Object imageData = task.getInputData().get("imageData");
        if (imageData == null) {
            throw new IllegalArgumentException("imageData parameter is required");
        }
    }

    @Override
    public int getThreadCount() {
        return 2; // Allow parallel NDVI calculations
    }
}

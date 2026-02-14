package com.enit.satellite_platform.modules.workflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Worker for load_image tasks.
 * Fetches satellite data from GEE (Google Earth Engine) service.
 */
@Slf4j
@Component
public class GEEDataWorker extends BaseTaskWorker {

    private final RestTemplate restTemplate;
    
    @Value("${gee.service.url:http://localhost:5000}")
    private String geeServiceUrl;

    public GEEDataWorker() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String getTaskDefName() {
        return "load_image";
    }

    @Override
    protected Map<String, Object> executeTask(Task task) throws Exception {
        log.info("GEEDataWorker: Fetching satellite image");
        
        // Get required parameters
        String imageId = (String) getRequiredInput(task, "imageId");
        String projectId = (String) getOptionalInput(task, "projectId", "default");
        
        // Optional parameters
        String startDate = (String) getOptionalInput(task, "startDate", null);
        String endDate = (String) getOptionalInput(task, "endDate", null);
        Object region = getOptionalInput(task, "region", null);
        
        log.info("GEEDataWorker: ImageId={}, Project={}", imageId, projectId);
        
        // Prepare request to GEE service
        Map<String, Object> geeRequest = new HashMap<>();
        geeRequest.put("imageId", imageId);
        geeRequest.put("projectId", projectId);
        if (startDate != null) geeRequest.put("startDate", startDate);
        if (endDate != null) geeRequest.put("endDate", endDate);
        if (region != null) geeRequest.put("region", region);
        
        try {
            // Call GEE service API
            String url = geeServiceUrl + "/api/gee/fetch";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(geeRequest, headers);
            
            log.info("GEEDataWorker: Calling GEE service at {}", url);
            ResponseEntity<Map> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                Map.class
            );
            
            Map<String, Object> geeResponse = response.getBody();
            if (geeResponse == null) {
                throw new RuntimeException("GEE service returned empty response");
            }
            
            // Create output with GEE data
            Map<String, Object> output = createOutput();
            output.put("imageId", imageId);
            output.put("imageData", geeResponse);
            output.put("imageUrl", geeResponse.get("url"));
            output.put("metadata", geeResponse.get("metadata"));
            output.put("status", "loaded");
            
            log.info("GEEDataWorker: Successfully fetched satellite image");
            return output;
            
        } catch (Exception e) {
            log.error("GEEDataWorker: Failed to fetch from GEE service: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch satellite image: " + e.getMessage(), e);
        }
    }

    @Override
    protected void validateInput(Task task) throws IllegalArgumentException {
        super.validateInput(task);
        
        String imageId = (String) task.getInputData().get("imageId");
        if (imageId == null || imageId.trim().isEmpty()) {
            throw new IllegalArgumentException("imageId parameter is required and cannot be empty");
        }
    }

    @Override
    public int getThreadCount() {
        return 3; // Allow parallel GEE requests
    }
}

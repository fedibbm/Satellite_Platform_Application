package com.enit.satellite_platform.modules.workflow.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.metadata.tasks.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Worker for calculate_ndvi tasks.
 * Processes satellite images and calculates NDVI (Normalized Difference Vegetation Index).
 */
@Slf4j
@Component
public class NDVIProcessingWorker extends BaseTaskWorker {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${image.processing.url:http://localhost:8000}")
    private String imageProcessingUrl;

    public NDVIProcessingWorker() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
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
        String redBand = (String) getOptionalInput(task, "redBand", "B4");
        String nirBand = (String) getOptionalInput(task, "nirBand", "B8");
        
        log.info("NDVIProcessingWorker: ImageId={}, RedBand={}, NirBand={}", 
            imageId, redBand, nirBand);
        
        try {
            // Extract preview URL from imageData
            String previewUrl = extractPreviewUrl(imageData);
            if (previewUrl == null) {
                throw new RuntimeException("No preview URL found in image data");
            }
            
            log.info("NDVIProcessingWorker: Downloading image from: {}", previewUrl);
            
            // Download image from GEE preview URL
            byte[] imageBytes = restTemplate.getForObject(previewUrl, byte[].class);
            if (imageBytes == null || imageBytes.length == 0) {
                throw new RuntimeException("Failed to download image from GEE");
            }
            
            log.info("NDVIProcessingWorker: Downloaded {} bytes", imageBytes.length);
            
            // Prepare multipart request
            String url = imageProcessingUrl + "/calculate/ndvi";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // Create multipart body
            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            
            // Add image file
            org.springframework.core.io.ByteArrayResource fileResource = new org.springframework.core.io.ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return "satellite_image.tif";
                }
            };
            body.add("file", fileResource);
            
            // Add metadata as JSON
            Map<String, Object> metadata = new HashMap<>();
            // Preview images from GEE typically have 4 bands: R, G, B, NIR
            // Use band 1 for Red and band 4 for NIR (not the original B4/B8 indices)
            metadata.put("redBand", 1);  // Red band in preview
            metadata.put("nirBand", 4);  // NIR band in preview (typically the 4th band)
            body.add("metadata", objectMapper.writeValueAsString(metadata));
            
            HttpEntity<org.springframework.util.MultiValueMap<String, Object>> request = 
                new HttpEntity<>(body, headers);
            
            log.info("NDVIProcessingWorker: Calling image processing service at {}", url);
            
            // First call returns multipart response with metadata + image
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );
            
            if (response.getBody() == null) {
                throw new RuntimeException("Image processing service returned empty response");
            }
            
            // Extract filename from response (it's in the Content-Disposition header or in the multipart)
            // The filename format is: upload_{timestamp}_{random}_ndvi.png
            String responseBody = response.getBody();
            String filename = null;
            
            // Try to extract filename from Content-Disposition in the multipart
            if (responseBody.contains("Content-Disposition: attachment; filename=")) {
                int filenameStart = responseBody.indexOf("filename=") + 9;
                int filenameEnd = responseBody.indexOf("\r\n", filenameStart);
                if (filenameEnd == -1) filenameEnd = responseBody.indexOf("\n", filenameStart);
                if (filenameEnd > filenameStart) {
                    filename = responseBody.substring(filenameStart, filenameEnd).trim();
                }
            }
            
            if (filename == null) {
                throw new RuntimeException("Could not extract filename from response");
            }
            
            log.info("NDVIProcessingWorker: NDVI image filename: {}", filename);
            
            // Now download the actual PNG file using the download endpoint
            String downloadUrl = imageProcessingUrl + "/download/" + filename;
            byte[] ndviImageBytes = restTemplate.getForObject(downloadUrl, byte[].class);
            
            if (ndviImageBytes == null || ndviImageBytes.length == 0) {
                throw new RuntimeException("Failed to download NDVI image from processing service");
            }
            
            // Save NDVI image to file system
            String outputDir = "upload-dir/ndvi-results";
            java.nio.file.Path outputPath = java.nio.file.Paths.get(outputDir);
            java.nio.file.Files.createDirectories(outputPath);
            
            String fileName = String.format("ndvi_%s_%s.png", 
                imageId.replaceAll("[^a-zA-Z0-9_-]", "_"),
                java.time.Instant.now().toEpochMilli());
            java.nio.file.Path filePath = outputPath.resolve(fileName);
            
            java.nio.file.Files.write(filePath, ndviImageBytes);
            
            String ndviUrl = "/api/files/ndvi-results/" + fileName;
            
            log.info("NDVIProcessingWorker: Saved NDVI image to {}", filePath);
            
            // Create output with NDVI results
            Map<String, Object> output = createOutput();
            output.put("status", "success");
            output.put("imageId", imageId);
            output.put("redBand", redBand);
            output.put("nirBand", nirBand);
            output.put("message", "NDVI calculation completed successfully");
            output.put("ndviImageSize", ndviImageBytes.length);
            output.put("ndviImagePath", filePath.toString());
            output.put("ndviImageUrl", ndviUrl);
            output.put("processedAt", java.time.Instant.now().toString());
            
            log.info("NDVIProcessingWorker: NDVI calculation completed successfully");
            return output;
            
        } catch (Exception e) {
            log.error("NDVIProcessingWorker: Failed to process image: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate NDVI: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract preview URL from nested imageData structure
     */
    @SuppressWarnings("unchecked")
    private String extractPreviewUrl(Object imageData) {
        try {
            if (imageData instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) imageData;
                
                // Try imageData.imageData.images[0].previewUrl
                Object nested = data.get("imageData");
                if (nested instanceof Map) {
                    Map<String, Object> nestedMap = (Map<String, Object>) nested;
                    Object images = nestedMap.get("images");
                    if (images instanceof java.util.List) {
                        java.util.List<?> imagesList = (java.util.List<?>) images;
                        if (!imagesList.isEmpty() && imagesList.get(0) instanceof Map) {
                            Map<String, Object> firstImage = (Map<String, Object>) imagesList.get(0);
                            Object url = firstImage.get("previewUrl");
                            if (url != null) {
                                return url.toString();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract preview URL: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Parse band name to numeric index (e.g., "B4" -> 4, "B8" -> 8)
     */
    private int parseBandIndex(String bandName) {
        if (bandName == null || bandName.isEmpty()) {
            return 1; // default
        }
        
        // Remove 'B' prefix if present and parse number
        String numStr = bandName.toUpperCase().replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            log.warn("Could not parse band index from '{}', using default 1", bandName);
            return 1;
        }
    }
    
    /**
     * Extract PNG image from multipart response
     */
    private byte[] extractImageFromMultipart(byte[] multipartData) {
        try {
            String responseStr = new String(multipartData, StandardCharsets.UTF_8);
            
            // Find the boundary marker
            String boundaryMarker = "--boundary";
            
            // Split by boundary and find the image part (has Content-Type: image/png)
            String[] parts = responseStr.split(boundaryMarker);
            
            for (String part : parts) {
                if (part.contains("Content-Type: image/png")) {
                    // Find where the actual image data starts (after the headers)
                    int imageStart = part.indexOf("\r\n\r\n");
                    if (imageStart > 0) {
                        imageStart += 4; // Skip the \r\n\r\n
                        
                        // Find where this part ends (next boundary or end marker)
                        int imageEnd = part.length();
                        
                        // Extract the image data as bytes
                        byte[] imagePart = part.substring(imageStart).getBytes(StandardCharsets.ISO_8859_1);
                        
                        // The image data might have trailing boundary markers, remove them
                        int actualEnd = imagePart.length;
                        for (int i = imagePart.length - 1; i >= 0; i--) {
                            if (imagePart[i] != '\r' && imagePart[i] != '\n' && imagePart[i] != '-') {
                                actualEnd = i + 1;
                                break;
                            }
                        }
                        
                        byte[] cleanImageData = new byte[actualEnd];
                        System.arraycopy(imagePart, 0, cleanImageData, 0, actualEnd);
                        
                        return cleanImageData;
                    }
                }
            }
            
            log.warn("Could not find image/png part in multipart response");
            return null;
            
        } catch (Exception e) {
            log.error("Failed to extract image from multipart: {}", e.getMessage());
            return null;
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

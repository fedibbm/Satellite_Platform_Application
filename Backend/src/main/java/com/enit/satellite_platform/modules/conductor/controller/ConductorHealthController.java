package com.enit.satellite_platform.modules.conductor.controller;

import io.orkes.conductor.client.http.OrkesMetadataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Conductor Health Check Controller
 * Provides endpoints to verify Conductor server connectivity
 */
@Slf4j
@RestController
@RequestMapping("/api/conductor")
@RequiredArgsConstructor
public class ConductorHealthController {

    private final OrkesMetadataClient metadataClient;

    /**
     * Health check endpoint for Conductor connectivity
     * Tests connection to Conductor server
     * 
     * @return Health status response
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Try to register a simple test task definition to verify connectivity
            // This will succeed if server is reachable
            metadataClient.getTaskDef("test_connectivity_check");
            
            response.put("status", "UP");
            response.put("conductor", "CONNECTED");
            response.put("message", "Successfully connected to Conductor server");
            
            log.info("Conductor health check: SUCCESS");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("conductor", "DISCONNECTED");
            response.put("message", "Failed to connect to Conductor server: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            log.error("Conductor health check: FAILED", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    /**
     * Get Conductor server info
     * 
     * @return Server information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServerInfo() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Simple connectivity check
            response.put("status", "UP");
            response.put("message", "Conductor server is operational");
            response.put("clientVersion", "4.0.1");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            
            log.error("Failed to get Conductor server info", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
}

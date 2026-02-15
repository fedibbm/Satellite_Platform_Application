package com.enit.satellite_platform.modules.workflow.controllers;

import com.enit.satellite_platform.modules.workflow.services.WebhookTriggerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for webhook-triggered workflows
 * Provides public endpoints for external systems to trigger workflows
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {
    
    private final WebhookTriggerService webhookTriggerService;
    
    /**
     * Generic webhook endpoint
     * Accepts any HTTP method and triggers workflow based on trigger ID
     * 
     * URL format: /api/webhooks/trigger/{triggerId}
     * 
     * @param triggerId Trigger ID
     * @param request HTTP request
     * @param body Request body (optional)
     * @return Execution result
     */
    @RequestMapping(value = "/trigger/{triggerId}", 
                    method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @PathVariable String triggerId,
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        
        try {
            log.info("Webhook received: triggerId={}, method={}, path={}", 
                    triggerId, request.getMethod(), request.getRequestURI());
            
            // Extract request data
            String method = request.getMethod();
            Map<String, String> headers = extractHeaders(request);
            Map<String, String> queryParams = extractQueryParams(request);
            Map<String, String> pathParams = new HashMap<>();
            pathParams.put("triggerId", triggerId);
            String clientIp = getClientIp(request);
            
            // Process webhook
            WebhookTriggerService.WebhookExecutionResult result = webhookTriggerService.processWebhook(
                    triggerId, method, headers, queryParams, pathParams, body, clientIp);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            
            if (result.isSuccess()) {
                response.put("workflowExecutionId", result.getWorkflowExecutionId());
                response.put("triggerId", triggerId);
                response.put("timestamp", System.currentTimeMillis());
            }
            
            return ResponseEntity
                    .status(result.getStatusCode())
                    .body(response);
            
        } catch (Exception e) {
            log.error("Error handling webhook: triggerId={}", triggerId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Internal server error");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Webhook with custom path parameters
     * URL format: /api/webhooks/trigger/{triggerId}/path/{param1}/{param2}
     * 
     * @param triggerId Trigger ID
     * @param pathVars Path variables
     * @param request HTTP request
     * @param body Request body (optional)
     * @return Execution result
     */
    @RequestMapping(value = "/trigger/{triggerId}/path/**", 
                    method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Map<String, Object>> handleWebhookWithPath(
            @PathVariable String triggerId,
            @PathVariable Map<String, String> pathVars,
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        
        try {
            log.info("Webhook with path received: triggerId={}, method={}, path={}", 
                    triggerId, request.getMethod(), request.getRequestURI());
            
            // Extract request data
            String method = request.getMethod();
            Map<String, String> headers = extractHeaders(request);
            Map<String, String> queryParams = extractQueryParams(request);
            
            // Extract path parameters from URL
            String pathInfo = request.getRequestURI();
            String basePath = "/api/webhooks/trigger/" + triggerId + "/path/";
            String pathParamsStr = pathInfo.substring(pathInfo.indexOf(basePath) + basePath.length());
            Map<String, String> pathParams = new HashMap<>();
            pathParams.put("triggerId", triggerId);
            
            // Parse path parameters
            String[] pathParts = pathParamsStr.split("/");
            for (int i = 0; i < pathParts.length; i++) {
                pathParams.put("param" + (i + 1), pathParts[i]);
            }
            
            String clientIp = getClientIp(request);
            
            // Process webhook
            WebhookTriggerService.WebhookExecutionResult result = webhookTriggerService.processWebhook(
                    triggerId, method, headers, queryParams, pathParams, body, clientIp);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            
            if (result.isSuccess()) {
                response.put("workflowExecutionId", result.getWorkflowExecutionId());
                response.put("triggerId", triggerId);
                response.put("pathParams", pathParams);
                response.put("timestamp", System.currentTimeMillis());
            }
            
            return ResponseEntity
                    .status(result.getStatusCode())
                    .body(response);
            
        } catch (Exception e) {
            log.error("Error handling webhook with path: triggerId={}", triggerId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Internal server error");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint for webhooks
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Webhook Service");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Extract headers from request
     */
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName.toLowerCase(), request.getHeader(headerName));
        }
        
        return headers;
    }
    
    /**
     * Extract query parameters from request
     */
    private Map<String, String> extractQueryParams(HttpServletRequest request) {
        Map<String, String> queryParams = new HashMap<>();
        
        if (request.getQueryString() != null) {
            for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                queryParams.put(entry.getKey(), entry.getValue()[0]);
            }
        }
        
        return queryParams;
    }
    
    /**
     * Get client IP address
     * Handles proxies and load balancers
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // Handle multiple IPs (take first one)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
}

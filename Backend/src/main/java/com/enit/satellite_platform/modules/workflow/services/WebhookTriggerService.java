package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowTrigger;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowTriggerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for handling webhook-based workflow triggers
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookTriggerService {
    
    private final WorkflowTriggerRepository triggerRepository;
    private final WorkflowExecutionService executionService;
    
    /**
     * Process a webhook request and trigger workflow if matching trigger is found
     * 
     * @param triggerId Trigger ID from webhook URL
     * @param method HTTP method
     * @param headers Request headers
     * @param queryParams Query parameters
     * @param pathParams Path parameters
     * @param body Request body
     * @param clientIp Client IP address
     * @return Execution result
     */
    public WebhookExecutionResult processWebhook(
            String triggerId,
            String method,
            Map<String, String> headers,
            Map<String, String> queryParams,
            Map<String, String> pathParams,
            Map<String, Object> body,
            String clientIp) {
        
        try {
            log.info("Processing webhook: triggerId={}, method={}, clientIp={}", 
                    triggerId, method, clientIp);
            
            // Find trigger
            Optional<WorkflowTrigger> triggerOpt = triggerRepository.findById(triggerId);
            if (triggerOpt.isEmpty()) {
                log.warn("Webhook trigger not found: id={}", triggerId);
                return WebhookExecutionResult.error("Trigger not found", 404);
            }
            
            WorkflowTrigger trigger = triggerOpt.get();
            
            // Check if trigger is enabled
            if (!trigger.getEnabled()) {
                log.warn("Webhook trigger is disabled: id={}", triggerId);
                return WebhookExecutionResult.error("Trigger is disabled", 403);
            }
            
            // Check if it's a webhook trigger
            if (trigger.getType() != WorkflowTrigger.TriggerType.WEBHOOK) {
                log.warn("Trigger is not a webhook type: id={}, type={}", 
                        triggerId, trigger.getType());
                return WebhookExecutionResult.error("Invalid trigger type", 400);
            }
            
            // Validate webhook configuration
            if (trigger.getConfig() == null) {
                log.error("Webhook trigger missing configuration: id={}", triggerId);
                return WebhookExecutionResult.error("Trigger configuration missing", 500);
            }
            
            // Validate HTTP method
            if (!isMethodAllowed(trigger, method)) {
                log.warn("HTTP method not allowed: triggerId={}, method={}", triggerId, method);
                return WebhookExecutionResult.error("HTTP method not allowed", 405);
            }
            
            // Validate IP whitelist
            if (!isIpAllowed(trigger, clientIp)) {
                log.warn("IP not whitelisted: triggerId={}, ip={}", triggerId, clientIp);
                return WebhookExecutionResult.error("IP not allowed", 403);
            }
            
            // Validate webhook secret
            if (!validateWebhookSecret(trigger, headers, body)) {
                log.warn("Invalid webhook secret: triggerId={}", triggerId);
                return WebhookExecutionResult.error("Invalid webhook secret", 401);
            }
            
            // Validate required headers
            if (!validateRequiredHeaders(trigger, headers)) {
                log.warn("Missing required headers: triggerId={}", triggerId);
                return WebhookExecutionResult.error("Missing required headers", 400);
            }
            
            // Build input parameters
            Map<String, Object> inputs = buildInputParameters(
                    trigger, headers, queryParams, pathParams, body);
            
            // Execute workflow
            // Use workflowDefinitionId as workflow name for now
            String workflowName = "workflow_" + trigger.getWorkflowDefinitionId();
            
            String workflowExecutionId = executionService.startWorkflow(
                    workflowName,
                    1, // version
                    inputs,
                    trigger.getWorkflowDefinitionId(),
                    trigger.getProjectId(),
                    trigger.getCreatedBy()
            );
            
            // Update trigger statistics
            trigger.setLastExecutedAt(LocalDateTime.now());
            trigger.setExecutionCount(trigger.getExecutionCount() + 1);
            trigger.setLastExecutionStatus("SUCCESS");
            trigger.setLastExecutionWorkflowId(workflowExecutionId);
            trigger.setUpdatedAt(LocalDateTime.now());
            triggerRepository.save(trigger);
            
            log.info("Successfully executed workflow from webhook: triggerId={}, workflowId={}", 
                    triggerId, workflowExecutionId);
            
            return WebhookExecutionResult.success(workflowExecutionId, inputs);
            
        } catch (Exception e) {
            log.error("Error processing webhook: triggerId={}", triggerId, e);
            return WebhookExecutionResult.error("Internal server error: " + e.getMessage(), 500);
        }
    }
    
    /**
     * Check if HTTP method is allowed
     */
    private boolean isMethodAllowed(WorkflowTrigger trigger, String method) {
        String[] allowedMethods = trigger.getConfig().getAllowedMethods();
        if (allowedMethods == null || allowedMethods.length == 0) {
            return true; // Allow all methods if not specified
        }
        
        return Arrays.asList(allowedMethods).contains(method.toUpperCase());
    }
    
    /**
     * Check if client IP is allowed
     */
    private boolean isIpAllowed(WorkflowTrigger trigger, String clientIp) {
        String[] ipWhitelist = trigger.getConfig().getIpWhitelist();
        if (ipWhitelist == null || ipWhitelist.length == 0) {
            return true; // Allow all IPs if not specified
        }
        
        return Arrays.asList(ipWhitelist).contains(clientIp);
    }
    
    /**
     * Validate webhook secret
     * Supports multiple validation methods:
     * 1. Header-based secret (X-Webhook-Secret)
     * 2. HMAC signature (X-Webhook-Signature)
     */
    private boolean validateWebhookSecret(
            WorkflowTrigger trigger, 
            Map<String, String> headers, 
            Map<String, Object> body) {
        
        String expectedSecret = trigger.getConfig().getWebhookSecret();
        if (expectedSecret == null || expectedSecret.isEmpty()) {
            return true; // No secret validation required
        }
        
        // Method 1: Simple secret in header
        String providedSecret = headers.get("x-webhook-secret");
        if (providedSecret != null && providedSecret.equals(expectedSecret)) {
            return true;
        }
        
        // Method 2: HMAC signature
        String signature = headers.get("x-webhook-signature");
        if (signature != null) {
            try {
                String bodyJson = body != null ? body.toString() : "";
                String calculatedSignature = calculateHmacSha256(bodyJson, expectedSecret);
                return signature.equals(calculatedSignature);
            } catch (Exception e) {
                log.error("Error validating HMAC signature", e);
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Calculate HMAC SHA256 signature
     */
    private String calculateHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
    
    /**
     * Validate required headers
     */
    private boolean validateRequiredHeaders(WorkflowTrigger trigger, Map<String, String> headers) {
        Map<String, String> requiredHeaders = trigger.getConfig().getRequiredHeaders();
        if (requiredHeaders == null || requiredHeaders.isEmpty()) {
            return true;
        }
        
        for (Map.Entry<String, String> required : requiredHeaders.entrySet()) {
            String headerValue = headers.get(required.getKey().toLowerCase());
            if (headerValue == null || !headerValue.equals(required.getValue())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Build input parameters from webhook data
     */
    private Map<String, Object> buildInputParameters(
            WorkflowTrigger trigger,
            Map<String, String> headers,
            Map<String, String> queryParams,
            Map<String, String> pathParams,
            Map<String, Object> body) {
        
        Map<String, Object> inputs = new HashMap<>();
        
        // Add default inputs from trigger
        if (trigger.getDefaultInputs() != null) {
            inputs.putAll(trigger.getDefaultInputs());
        }
        
        // Add webhook metadata
        inputs.put("triggerId", trigger.getId());
        inputs.put("triggerType", "WEBHOOK");
        inputs.put("webhookTimestamp", LocalDateTime.now().toString());
        
        // Map path parameters
        if (trigger.getConfig().getPathParamMapping() != null) {
            for (Map.Entry<String, String> mapping : trigger.getConfig().getPathParamMapping().entrySet()) {
                String pathParam = mapping.getKey();
                String workflowParam = mapping.getValue();
                String value = pathParams.get(pathParam);
                if (value != null) {
                    inputs.put(workflowParam, value);
                }
            }
        }
        
        // Map query parameters
        if (trigger.getConfig().getQueryParamMapping() != null) {
            for (Map.Entry<String, String> mapping : trigger.getConfig().getQueryParamMapping().entrySet()) {
                String queryParam = mapping.getKey();
                String workflowParam = mapping.getValue();
                String value = queryParams.get(queryParam);
                if (value != null) {
                    inputs.put(workflowParam, value);
                }
            }
        }
        
        // Map body fields
        if (body != null && trigger.getConfig().getBodyMapping() != null) {
            for (Map.Entry<String, String> mapping : trigger.getConfig().getBodyMapping().entrySet()) {
                String bodyField = mapping.getKey();
                String workflowParam = mapping.getValue();
                Object value = body.get(bodyField);
                if (value != null) {
                    inputs.put(workflowParam, value);
                }
            }
        } else if (body != null && trigger.getConfig().getBodyMapping() == null) {
            // If no mapping, pass entire body
            inputs.putAll(body);
        }
        
        return inputs;
    }
    
    /**
     * Result of webhook execution
     */
    public static class WebhookExecutionResult {
        private final boolean success;
        private final String message;
        private final int statusCode;
        private final String workflowExecutionId;
        private final Map<String, Object> inputs;
        
        private WebhookExecutionResult(boolean success, String message, int statusCode, 
                                      String workflowExecutionId, Map<String, Object> inputs) {
            this.success = success;
            this.message = message;
            this.statusCode = statusCode;
            this.workflowExecutionId = workflowExecutionId;
            this.inputs = inputs;
        }
        
        public static WebhookExecutionResult success(String workflowExecutionId, Map<String, Object> inputs) {
            return new WebhookExecutionResult(true, "Workflow triggered successfully", 
                    200, workflowExecutionId, inputs);
        }
        
        public static WebhookExecutionResult error(String message, int statusCode) {
            return new WebhookExecutionResult(false, message, statusCode, null, null);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public int getStatusCode() {
            return statusCode;
        }
        
        public String getWorkflowExecutionId() {
            return workflowExecutionId;
        }
        
        public Map<String, Object> getInputs() {
            return inputs;
        }
    }
}

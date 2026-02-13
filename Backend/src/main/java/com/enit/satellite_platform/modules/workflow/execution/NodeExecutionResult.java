package com.enit.satellite_platform.modules.workflow.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionResult {
    private boolean success;
    private Object data;
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    private String message;
    
    public static NodeExecutionResult success(Object data) {
        return NodeExecutionResult.builder()
                .success(true)
                .data(data)
                .build();
    }
    
    public static NodeExecutionResult success(Object data, String message) {
        return NodeExecutionResult.builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }
    
    public static NodeExecutionResult failure(String error) {
        NodeExecutionResult result = NodeExecutionResult.builder()
                .success(false)
                .build();
        result.getErrors().add(error);
        return result;
    }
    
    public static NodeExecutionResult failure(List<String> errors) {
        return NodeExecutionResult.builder()
                .success(false)
                .errors(errors)
                .build();
    }
    
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.success = false;
    }
    
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }
    
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
}

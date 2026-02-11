package com.enit.satellite_platform.modules.workflow.execution;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of node execution
 */
public class NodeExecutionResult {
    private final boolean success;
    private final Object data;
    private final List<String> errors;
    private final List<String> warnings;

    private NodeExecutionResult(boolean success, Object data, List<String> errors, List<String> warnings) {
        this.success = success;
        this.data = data;
        this.errors = errors;
        this.warnings = warnings;
    }

    public static NodeExecutionResult success(Object data) {
        return new NodeExecutionResult(true, data, new ArrayList<>(), new ArrayList<>());
    }

    public static NodeExecutionResult failure(String error) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return new NodeExecutionResult(false, null, errors, new ArrayList<>());
    }

    public static NodeExecutionResult failure(List<String> errors) {
        return new NodeExecutionResult(false, null, errors, new ArrayList<>());
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}

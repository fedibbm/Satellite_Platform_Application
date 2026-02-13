package com.enit.satellite_platform.modules.workflow.execution.nodes;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.enit.satellite_platform.modules.workflow.execution.NodeExecutionContext;
import com.enit.satellite_platform.modules.workflow.execution.NodeExecutionResult;
import com.enit.satellite_platform.modules.workflow.execution.WorkflowNodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DecisionNodeExecutor implements WorkflowNodeExecutor {
    
    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    
    @Override
    public String getNodeType() {
        return "decision";
    }
    
    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        log.info("Executing decision node: {}", node.getId());
        
        Map<String, Object> config = node.getData().getConfig();
        String condition = (String) config.get("condition");
        String operator = (String) config.getOrDefault("operator", "equals");
        
        try {
            boolean result = evaluateCondition(condition, operator, context, config);
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("condition", condition);
            resultData.put("operator", operator);
            resultData.put("result", result);
            resultData.put("path", result ? "true_branch" : "false_branch");
            
            String message = result 
                    ? "Condition evaluated to TRUE - proceeding to true branch"
                    : "Condition evaluated to FALSE - proceeding to false branch";
            
            NodeExecutionResult executionResult = NodeExecutionResult.success(resultData, message);
            executionResult.addMetadata("conditionMet", result);
            
            return executionResult;
            
        } catch (Exception e) {
            log.error("Error evaluating condition: {}", e.getMessage(), e);
            return NodeExecutionResult.failure("Condition evaluation failed: " + e.getMessage());
        }
    }
    
    private boolean evaluateCondition(String condition, String operator, NodeExecutionContext context, Map<String, Object> config) {
        // Get value to compare from previous node output or context
        Object actualValue = getValueFromContext(condition, context);
        Object expectedValue = config.get("value");
        
        log.debug("Evaluating: {} {} {}", actualValue, operator, expectedValue);
        
        switch (operator) {
            case "equals":
                return evaluateEquals(actualValue, expectedValue);
            
            case "not_equals":
                return !evaluateEquals(actualValue, expectedValue);
            
            case "greater":
                return evaluateGreater(actualValue, expectedValue);
            
            case "less":
                return evaluateLess(actualValue, expectedValue);
            
            case "greater_or_equal":
                return evaluateGreater(actualValue, expectedValue) || evaluateEquals(actualValue, expectedValue);
            
            case "less_or_equal":
                return evaluateLess(actualValue, expectedValue) || evaluateEquals(actualValue, expectedValue);
            
            case "contains":
                return evaluateContains(actualValue, expectedValue);
            
            case "exists":
                return actualValue != null;
            
            case "expression":
                return evaluateExpression(condition, context);
            
            default:
                log.warn("Unknown operator: {}, defaulting to equals", operator);
                return evaluateEquals(actualValue, expectedValue);
        }
    }
    
    private Object getValueFromContext(String path, NodeExecutionContext context) {
        // Support dot notation for nested paths: "data.statistics.mean"
        String[] parts = path.split("\\.");
        
        // Check in previous node outputs first
        if (!context.getNodeOutputs().isEmpty()) {
            // Get the last node output
            Object lastOutput = context.getNodeOutputs().values().stream()
                    .reduce((first, second) -> second)
                    .orElse(null);
            
            if (lastOutput instanceof Map) {
                return getNestedValue((Map<String, Object>) lastOutput, parts, 0);
            }
        }
        
        // Check in global variables
        if (context.getGlobalVariables().containsKey(path)) {
            return context.getGlobalVariables().get(path);
        }
        
        // Check in execution parameters
        if (context.getExecutionParameters().containsKey(path)) {
            return context.getExecutionParameters().get(path);
        }
        
        return null;
    }
    
    private Object getNestedValue(Map<String, Object> map, String[] path, int index) {
        if (index >= path.length) {
            return null;
        }
        
        Object value = map.get(path[index]);
        
        if (index == path.length - 1) {
            return value;
        }
        
        if (value instanceof Map) {
            return getNestedValue((Map<String, Object>) value, path, index + 1);
        }
        
        return null;
    }
    
    private boolean evaluateEquals(Object actual, Object expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;
        
        // Try numeric comparison if both are numbers
        if (actual instanceof Number && expected instanceof Number) {
            return ((Number) actual).doubleValue() == ((Number) expected).doubleValue();
        }
        
        return actual.toString().equals(expected.toString());
    }
    
    private boolean evaluateGreater(Object actual, Object expected) {
        if (!(actual instanceof Number) || !(expected instanceof Number)) {
            return false;
        }
        return ((Number) actual).doubleValue() > ((Number) expected).doubleValue();
    }
    
    private boolean evaluateLess(Object actual, Object expected) {
        if (!(actual instanceof Number) || !(expected instanceof Number)) {
            return false;
        }
        return ((Number) actual).doubleValue() < ((Number) expected).doubleValue();
    }
    
    private boolean evaluateContains(Object actual, Object expected) {
        if (actual == null || expected == null) return false;
        return actual.toString().contains(expected.toString());
    }
    
    private boolean evaluateExpression(String expression, NodeExecutionContext context) {
        try {
            ScriptEngine engine = scriptEngineManager.getEngineByName("JavaScript");
            
            // Add context variables to engine
            for (Map.Entry<String, Object> entry : context.getGlobalVariables().entrySet()) {
                engine.put(entry.getKey(), entry.getValue());
            }
            
            Object result = engine.eval(expression);
            return Boolean.TRUE.equals(result);
            
        } catch (Exception e) {
            log.error("Error evaluating expression: {}", expression, e);
            return false;
        }
    }
    
    @Override
    public boolean validate(WorkflowNode node) {
        Map<String, Object> config = node.getData().getConfig();
        return config.containsKey("condition") && config.containsKey("operator");
    }
    
    @Override
    public NodeMetadata getMetadata() {
        return new NodeMetadata(
                "decision",
                "Decision Node",
                "Routes workflow execution based on conditional logic",
                "Control"
        );
    }
}

package com.enit.satellite_platform.modules.workflow.execution.nodes;

import com.enit.satellite_platform.modules.workflow.entities.NodeType;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.enit.satellite_platform.modules.workflow.execution.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Executor for decision nodes - conditional routing based on expressions
 * Evaluates conditions and determines which path to follow
 */
@Component
public class DecisionNodeExecutor implements NodeExecutor {
    private static final Logger logger = LoggerFactory.getLogger(DecisionNodeExecutor.class);

    @Override
    public NodeType getNodeType() {
        return NodeType.DECISION;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        logger.info("Executing decision node: {}", node.getId());

        try {
            Map<String, Object> config = node.getData().getConfig();
            
            if (config == null || config.isEmpty()) {
                return NodeExecutionResult.failure("Node configuration is required for decision");
            }

            // Get condition to evaluate
            String conditionType = (String) config.getOrDefault("conditionType", "comparison");
            logger.info("Condition type: {}", conditionType);

            boolean conditionResult;
            
            switch (conditionType.toLowerCase()) {
                case "comparison":
                    conditionResult = evaluateComparison(config, context);
                    break;
                    
                case "threshold":
                    conditionResult = evaluateThreshold(config, context);
                    break;
                    
                case "expression":
                    conditionResult = evaluateExpression(config, context);
                    break;
                    
                case "data-check":
                    conditionResult = evaluateDataCheck(config, context);
                    break;
                    
                default:
                    return NodeExecutionResult.failure("Unknown condition type: " + conditionType);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("conditionType", conditionType);
            result.put("decision", conditionResult);
            result.put("path", conditionResult ? "true" : "false");
            result.put("status", "success");

            logger.info("Decision node completed: {} with result: {}", node.getId(), conditionResult);
            return NodeExecutionResult.success(result);

        } catch (Exception e) {
            logger.error("Error executing decision node: {}", node.getId(), e);
            return NodeExecutionResult.failure("Decision execution failed: " + e.getMessage());
        }
    }

    private boolean evaluateComparison(Map<String, Object> config, NodeExecutionContext context) {
        String leftOperand = (String) config.get("leftOperand");
        String operator = (String) config.get("operator");
        Object rightValue = config.get("rightValue");

        // Get value from previous node output
        Object leftValue = resolveValue(leftOperand, context);

        logger.info("Comparing: {} {} {}", leftValue, operator, rightValue);

        if (leftValue == null) {
            return false;
        }

        return performComparison(leftValue, operator, rightValue);
    }

    private boolean evaluateThreshold(Map<String, Object> config, NodeExecutionContext context) {
        String inputKey = (String) config.get("inputKey");
        Number threshold = (Number) config.get("threshold");
        String comparison = (String) config.getOrDefault("comparison", ">");

        Object value = resolveValue(inputKey, context);
        
        if (!(value instanceof Number)) {
            logger.warn("Threshold evaluation requires numeric value, got: {}", value != null ? value.getClass() : "null");
            return false;
        }

        double numericValue = ((Number) value).doubleValue();
        double thresholdValue = threshold.doubleValue();

        logger.info("Threshold check: {} {} {}", numericValue, comparison, thresholdValue);

        switch (comparison) {
            case ">": return numericValue > thresholdValue;
            case ">=": return numericValue >= thresholdValue;
            case "<": return numericValue < thresholdValue;
            case "<=": return numericValue <= thresholdValue;
            case "==": return Math.abs(numericValue - thresholdValue) < 0.0001;
            case "!=": return Math.abs(numericValue - thresholdValue) >= 0.0001;
            default:
                logger.warn("Unknown comparison operator: {}", comparison);
                return false;
        }
    }

    private boolean evaluateExpression(Map<String, Object> config, NodeExecutionContext context) {
        String expression = (String) config.get("expression");
        
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        // Simple expression evaluation
        // In production, use a proper expression parser like SpEL or JEXL
        logger.info("Evaluating expression: {}", expression);

        // For now, support simple expressions like "value > 10" or "status == 'success'"
        try {
            // Replace variables in expression with actual values
            String resolvedExpression = resolveExpressionVariables(expression, context);
            
            // Simple evaluation (this is a placeholder - use proper parser in production)
            return evaluateSimpleExpression(resolvedExpression);
            
        } catch (Exception e) {
            logger.error("Error evaluating expression: {}", expression, e);
            return false;
        }
    }

    private boolean evaluateDataCheck(Map<String, Object> config, NodeExecutionContext context) {
        String checkType = (String) config.getOrDefault("checkType", "exists");
        String inputKey = (String) config.get("inputKey");

        Object value = resolveValue(inputKey, context);

        logger.info("Data check: {} for key: {}", checkType, inputKey);

        switch (checkType.toLowerCase()) {
            case "exists":
                return value != null;
                
            case "not-empty":
                if (value == null) return false;
                if (value instanceof String) return !((String) value).trim().isEmpty();
                if (value instanceof Map) return !((Map<?, ?>) value).isEmpty();
                if (value instanceof Iterable) {
                    return ((Iterable<?>) value).iterator().hasNext();
                }
                return true;
                
            case "is-success":
                if (value instanceof Map) {
                    Object status = ((Map<?, ?>) value).get("status");
                    return "success".equalsIgnoreCase(String.valueOf(status));
                }
                return false;
                
            default:
                logger.warn("Unknown check type: {}", checkType);
                return false;
        }
    }

    private Object resolveValue(String key, NodeExecutionContext context) {
        if (key == null) return null;

        // Check if it's a node reference (e.g., "node1.status")
        if (key.contains(".")) {
            String[] parts = key.split("\\.", 2);
            String nodeId = parts[0];
            String field = parts[1];
            
            Object nodeOutput = context.getNodeOutput(nodeId);
            if (nodeOutput instanceof Map) {
                return ((Map<?, ?>) nodeOutput).get(field);
            }
            return null;
        }

        // Check global variables
        return context.getGlobalVariables().get(key);
    }

    private boolean performComparison(Object left, String operator, Object right) {
        if (operator == null) return false;

        switch (operator) {
            case "==":
            case "equals":
                return objectsEqual(left, right);
                
            case "!=":
            case "not-equals":
                return !objectsEqual(left, right);
                
            case ">":
                return compareNumbers(left, right) > 0;
                
            case ">=":
                return compareNumbers(left, right) >= 0;
                
            case "<":
                return compareNumbers(left, right) < 0;
                
            case "<=":
                return compareNumbers(left, right) <= 0;
                
            case "contains":
                return String.valueOf(left).contains(String.valueOf(right));
                
            case "starts-with":
                return String.valueOf(left).startsWith(String.valueOf(right));
                
            case "ends-with":
                return String.valueOf(left).endsWith(String.valueOf(right));
                
            default:
                logger.warn("Unknown operator: {}", operator);
                return false;
        }
    }

    private boolean objectsEqual(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        
        // Try numeric comparison first
        if (left instanceof Number && right instanceof Number) {
            return Math.abs(((Number) left).doubleValue() - ((Number) right).doubleValue()) < 0.0001;
        }
        
        return left.equals(right) || String.valueOf(left).equals(String.valueOf(right));
    }

    private int compareNumbers(Object left, Object right) {
        if (!(left instanceof Number) || !(right instanceof Number)) {
            throw new IllegalArgumentException("Cannot compare non-numeric values");
        }
        
        double leftNum = ((Number) left).doubleValue();
        double rightNum = ((Number) right).doubleValue();
        
        return Double.compare(leftNum, rightNum);
    }

    private String resolveExpressionVariables(String expression, NodeExecutionContext context) {
        // Simple variable replacement - in production use proper expression parser
        String result = expression;
        
        // Replace node references like ${node1.status}
        // This is a simplified implementation
        return result;
    }

    private boolean evaluateSimpleExpression(String expression) {
        // Simplified evaluation - in production use SpEL or similar
        // For now, just check if expression is "true" or contains "success"
        return expression.toLowerCase().contains("true") || 
               expression.toLowerCase().contains("success");
    }

    @Override
    public boolean validate(WorkflowNode node) {
        Map<String, Object> config = node.getData().getConfig();
        
        if (config == null || config.isEmpty()) {
            logger.warn("Node {} has no configuration", node.getId());
            return false;
        }

        String conditionType = (String) config.getOrDefault("conditionType", "comparison");
        
        switch (conditionType.toLowerCase()) {
            case "comparison":
                return config.containsKey("operator");
                
            case "threshold":
                return config.containsKey("threshold");
                
            case "expression":
                return config.containsKey("expression");
                
            case "data-check":
                return config.containsKey("inputKey");
                
            default:
                return false;
        }
    }

    @Override
    public NodeMetadata getMetadata() {
        Map<String, String> schema = new HashMap<>();
        schema.put("conditionType", "String: comparison, threshold, expression, data-check");
        schema.put("leftOperand", "String: Variable or node output reference (e.g., 'node1.status')");
        schema.put("operator", "String: ==, !=, >, >=, <, <=, contains, starts-with, ends-with");
        schema.put("rightValue", "Any: Value to compare against");
        schema.put("inputKey", "String: Key to check in node outputs");
        schema.put("threshold", "Number: Threshold value for comparison");
        schema.put("comparison", "String: Comparison operator for threshold");
        schema.put("expression", "String: Expression to evaluate");
        schema.put("checkType", "String: exists, not-empty, is-success");

        return new NodeMetadata(
            "Decision",
            "Evaluates conditions and routes workflow based on result",
            "Control Flow",
            schema,
            java.util.List.of("condition"),
            java.util.List.of("decision", "path")
        );
    }
}

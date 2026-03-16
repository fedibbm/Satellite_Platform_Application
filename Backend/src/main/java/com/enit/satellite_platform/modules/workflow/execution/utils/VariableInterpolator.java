package com.enit.satellite_platform.modules.workflow.execution.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableInterpolator {

    // Matches strings like {{nodes.node_1.output.bounds}} or {{global.projectId}}
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\{\\{([^\\}]+)\\}\\}");

    /**
     * Deeply resolves a map of properties, evaluating any string values that contain expressions.
     */
    public static Map<String, Object> resolveMap(Map<String, Object> input, Map<String, Object> contextData) {
        if (input == null) return null;
        Map<String, Object> resolved = new HashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            resolved.put(entry.getKey(), resolveValue(entry.getValue(), contextData));
        }
        return resolved;
    }

    @SuppressWarnings("unchecked")
    private static Object resolveValue(Object value, Map<String, Object> contextData) {
        if (value instanceof String) {
            String strValue = (String) value;
            // If the entire string is one expression, we can return the raw object (e.g., a Map or List)
            Matcher fullMatcher = Pattern.compile("^\\{\\{([^\\}]+)\\}\\}$").matcher(strValue.trim());
            if (fullMatcher.matches()) {
                return getNestedValue(contextData, fullMatcher.group(1).trim());
            }

            // Otherwise, replace interpolations within the string
            Matcher matcher = EXPRESSION_PATTERN.matcher(strValue);
            boolean hasMatches = false;
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                hasMatches = true;
                String path = matcher.group(1).trim();
                Object extracted = getNestedValue(contextData, path);
                String replacement = extracted != null ? extracted.toString() : "";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            if (hasMatches) {
                matcher.appendTail(sb);
                return sb.toString();
            }
            return value;
        } else if (value instanceof Map) {
            return resolveMap((Map<String, Object>) value, contextData);
        } else if (value instanceof List) {
            List<Object> resolvedList = new ArrayList<>();
            for (Object item : (List<?>) value) {
                resolvedList.add(resolveValue(item, contextData));
            }
            return resolvedList;
        }
        return value; // Numbers, Booleans, etc.
    }

    @SuppressWarnings("unchecked")
    private static Object getNestedValue(Map<String, Object> contextData, String path) {
        String[] parts = path.split("\\.");
        Object current = contextData;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null; // Path broken
            }
        }
        return current;
    }
}
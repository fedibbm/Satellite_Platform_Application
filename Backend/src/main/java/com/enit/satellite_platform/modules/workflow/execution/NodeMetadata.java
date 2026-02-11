package com.enit.satellite_platform.modules.workflow.execution;

import java.util.List;
import java.util.Map;

/**
 * Metadata describing a node type
 */
public class NodeMetadata {
    private final String name;
    private final String description;
    private final String category;
    private final Map<String, String> configSchema;
    private final List<String> requiredInputs;
    private final List<String> outputs;

    public NodeMetadata(String name, String description, String category,
                       Map<String, String> configSchema, List<String> requiredInputs, List<String> outputs) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.configSchema = configSchema;
        this.requiredInputs = requiredInputs;
        this.outputs = outputs;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public Map<String, String> getConfigSchema() {
        return configSchema;
    }

    public List<String> getRequiredInputs() {
        return requiredInputs;
    }

    public List<String> getOutputs() {
        return outputs;
    }
}

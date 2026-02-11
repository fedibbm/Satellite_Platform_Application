package com.enit.satellite_platform.modules.workflow.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum NodeType {
    @JsonProperty("trigger")
    TRIGGER,
    
    @JsonProperty("data-input")
    DATA_INPUT,
    
    @JsonProperty("processing")
    PROCESSING,
    
    @JsonProperty("decision")
    DECISION,
    
    @JsonProperty("output")
    OUTPUT
}

package com.enit.satellite_platform.modules.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteWorkflowRequest {
    private Map<String, Object> parameters = new HashMap<>();
}

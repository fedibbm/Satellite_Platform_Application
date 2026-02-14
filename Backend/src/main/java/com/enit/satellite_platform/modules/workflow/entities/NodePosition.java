package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Node Position
 * Stores UI coordinates for ReactFlow
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodePosition {
    
    private Double x;
    
    private Double y;
}

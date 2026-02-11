package com.enit.satellite_platform.modules.workflow.entities;

import lombok.Data;
import java.util.Map;

@Data
public class Position {
    private double x;
    private double y;

    public Position() {}

    public Position(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

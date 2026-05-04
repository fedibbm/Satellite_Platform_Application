package com.enit.satellite_platform.modules.user_management.admin_privileges.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private long totalUsers;
    private long totalProjects;
    private long pendingSignups;
}

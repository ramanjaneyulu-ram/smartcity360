package com.smartcity360.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class AnalyticsResponse {
    private long totalComplaints;
    private long resolvedCount;
    private long inProgressCount;
    private long unassignedCount;
    private double avgResolutionDays;
    private Map<String, Long> complaintsByCategory;
    private Map<String, Long> complaintsByDepartment;
    private List<MonthlyPoint> monthlyTrend;

    @Data
    @Builder
    @AllArgsConstructor
    public static class MonthlyPoint {
        private String month;
        private long filed;
        private long resolved;
    }
}

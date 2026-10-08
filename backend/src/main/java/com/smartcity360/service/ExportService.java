package com.smartcity360.service;

import com.smartcity360.dto.AnalyticsResponse;
import com.smartcity360.model.Complaint;
import com.smartcity360.repository.AssignmentRepository;
import com.smartcity360.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ComplaintRepository complaintRepository;
    private final AssignmentRepository assignmentRepository;
    private final AnalyticsService analyticsService;

    public byte[] exportComplaintsCsv(String department) {
        List<Complaint> list = department == null || department.isBlank()
                ? complaintRepository.findAllByOrderByCreatedAtDesc()
                : complaintRepository.findByDepartmentOrderByCreatedAtDesc(department);

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM for Microsoft Excel compatibility
        sb.append('\ufeff');

        // Headers
        sb.append("ID,Public ID,Category,Department,Priority,Status,Citizen Name,Citizen Email,Location,Assigned Officer,Created At,Updated At\n");

        for (Complaint c : list) {
            String publicId = "SC-" + (20000 + c.getId());
            String officer = assignmentRepository.findByComplaint(c)
                    .map(a -> a.getOfficer().getName())
                    .orElse("Unassigned");

            sb.append(c.getId()).append(",");
            sb.append(escapeCsv(publicId)).append(",");
            sb.append(escapeCsv(c.getCategory())).append(",");
            sb.append(escapeCsv(c.getDepartment())).append(",");
            sb.append(escapeCsv(c.getPriority() != null ? c.getPriority().name() : "")).append(",");
            sb.append(escapeCsv(c.getStatus() != null ? c.getStatus().name() : "")).append(",");
            sb.append(escapeCsv(c.getCitizen() != null ? c.getCitizen().getName() : "")).append(",");
            sb.append(escapeCsv(c.getCitizen() != null ? c.getCitizen().getEmail() : "")).append(",");
            sb.append(escapeCsv(c.getLocation())).append(",");
            sb.append(escapeCsv(officer)).append(",");
            sb.append(escapeCsv(c.getCreatedAt() != null ? c.getCreatedAt().format(DATE_FORMAT) : "")).append(",");
            sb.append(escapeCsv(c.getUpdatedAt() != null ? c.getUpdatedAt().format(DATE_FORMAT) : "")).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportAnalyticsSummaryCsv() {
        AnalyticsResponse analytics = analyticsService.buildAnalytics();
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');

        sb.append("=== SMARTCITY 360 CITY-WIDE ANALYTICS SUMMARY ===\n\n");
        sb.append("KPI,Value\n");
        sb.append("Total Grievances,").append(analytics.getTotalComplaints()).append("\n");
        sb.append("Resolved,").append(analytics.getResolvedCount()).append("\n");
        sb.append("In Progress,").append(analytics.getInProgressCount()).append("\n");
        sb.append("Unassigned,").append(analytics.getUnassignedCount()).append("\n");
        sb.append("Avg Resolution Time (Days),").append(analytics.getAvgResolutionDays()).append("\n\n");

        sb.append("=== COMPLAINTS BY CATEGORY ===\n");
        sb.append("Category,Count\n");
        for (Map.Entry<String, Long> entry : analytics.getComplaintsByCategory().entrySet()) {
            sb.append(escapeCsv(entry.getKey())).append(",").append(entry.getValue()).append("\n");
        }
        sb.append("\n");

        sb.append("=== COMPLAINTS BY DEPARTMENT ===\n");
        sb.append("Department,Count\n");
        for (Map.Entry<String, Long> entry : analytics.getComplaintsByDepartment().entrySet()) {
            sb.append(escapeCsv(entry.getKey())).append(",").append(entry.getValue()).append("\n");
        }
        sb.append("\n");

        sb.append("=== MONTHLY TREND ===\n");
        sb.append("Month,Filed,Resolved\n");
        for (AnalyticsResponse.MonthlyPoint point : analytics.getMonthlyTrend()) {
            sb.append(escapeCsv(point.getMonth())).append(",")
                    .append(point.getFiled()).append(",")
                    .append(point.getResolved()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String stringValue = value.replace("\r", " ").replace("\n", " ");
        if (stringValue.contains(",") || stringValue.contains("\"") || stringValue.contains(";")) {
            return "\"" + stringValue.replace("\"", "\"\"") + "\"";
        }
        return stringValue;
    }
}

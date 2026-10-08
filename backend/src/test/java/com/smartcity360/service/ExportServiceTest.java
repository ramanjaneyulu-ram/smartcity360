package com.smartcity360.service;

import com.smartcity360.dto.AnalyticsResponse;
import com.smartcity360.model.Complaint;
import com.smartcity360.model.ComplaintStatus;
import com.smartcity360.model.Priority;
import com.smartcity360.model.User;
import com.smartcity360.repository.AssignmentRepository;
import com.smartcity360.repository.ComplaintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ExportServiceTest {

    private ComplaintRepository complaintRepository;
    private AssignmentRepository assignmentRepository;
    private AnalyticsService analyticsService;
    private ExportService exportService;

    @BeforeEach
    void setUp() {
        complaintRepository = mock(ComplaintRepository.class);
        assignmentRepository = mock(AssignmentRepository.class);
        analyticsService = mock(AnalyticsService.class);
        exportService = new ExportService(complaintRepository, assignmentRepository, analyticsService);
    }

    @Test
    void exportComplaintsCsvGeneratesValidCsv() {
        User citizen = User.builder().name("John Citizen").email("john@example.com").build();
        Complaint complaint = Complaint.builder()
                .id(1L)
                .category("Road Damage")
                .department("Public Works")
                .priority(Priority.HIGH)
                .status(ComplaintStatus.IN_PROGRESS)
                .location("123 Main St, Central")
                .citizen(citizen)
                .createdAt(LocalDateTime.of(2026, 3, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 2, 12, 0))
                .build();

        when(complaintRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(complaint));
        when(assignmentRepository.findByComplaint(complaint)).thenReturn(Optional.empty());

        byte[] result = exportService.exportComplaintsCsv(null);

        assertNotNull(result);
        String csv = new String(result, StandardCharsets.UTF_8);
        assertTrue(csv.contains("ID,Public ID,Category,Department,Priority,Status,Citizen Name,Citizen Email,Location,Assigned Officer,Created At,Updated At"));
        assertTrue(csv.contains("SC-20001"));
        assertTrue(csv.contains("Road Damage"));
        assertTrue(csv.contains("John Citizen"));
        assertTrue(csv.contains("Unassigned"));
    }

    @Test
    void exportAnalyticsSummaryCsvGeneratesValidSummary() {
        AnalyticsResponse response = AnalyticsResponse.builder()
                .totalComplaints(25L)
                .resolvedCount(15L)
                .inProgressCount(5L)
                .unassignedCount(5L)
                .avgResolutionDays(2.5)
                .complaintsByCategory(Map.of("Road Damage", 10L, "Street Light", 15L))
                .complaintsByDepartment(Map.of("Public Works", 10L, "Electrical", 15L))
                .monthlyTrend(Collections.emptyList())
                .build();

        when(analyticsService.buildAnalytics()).thenReturn(response);

        byte[] result = exportService.exportAnalyticsSummaryCsv();

        assertNotNull(result);
        String csv = new String(result, StandardCharsets.UTF_8);
        assertTrue(csv.contains("=== SMARTCITY 360 CITY-WIDE ANALYTICS SUMMARY ==="));
        assertTrue(csv.contains("Total Grievances,25"));
        assertTrue(csv.contains("Avg Resolution Time (Days),2.5"));
        assertTrue(csv.contains("Road Damage,10"));
    }
}

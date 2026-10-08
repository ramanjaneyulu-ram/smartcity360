package com.smartcity360.controller;

import com.smartcity360.dto.AnalyticsResponse;
import com.smartcity360.dto.OfficerCreateRequest;
import com.smartcity360.dto.OfficerResponse;
import com.smartcity360.model.Role;
import com.smartcity360.repository.UserRepository;
import com.smartcity360.service.AuthService;
import com.smartcity360.service.AnalyticsService;
import com.smartcity360.service.ExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final ExportService exportService;

    @GetMapping("/analytics")
    public AnalyticsResponse analytics() {
        return analyticsService.buildAnalytics();
    }

    @GetMapping("/export/complaints/csv")
    public ResponseEntity<byte[]> exportComplaintsCsv(@RequestParam(required = false) String department) {
        byte[] csvData = exportService.exportComplaintsCsv(department);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"smartcity360_complaints.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping("/export/analytics/csv")
    public ResponseEntity<byte[]> exportAnalyticsCsv() {
        byte[] csvData = exportService.exportAnalyticsSummaryCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"smartcity360_analytics_summary.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping("/officers")
    public List<OfficerResponse> officers() {
        return userRepository.findByRole(Role.OFFICER).stream().map(OfficerResponse::from).toList();
    }

    @PostMapping("/officers")
    public OfficerResponse createOfficer(@Valid @RequestBody OfficerCreateRequest req) {
        return authService.createOfficer(req);
    }
}

package com.smartcity360.service;

import com.smartcity360.dto.AnalyticsResponse;
import com.smartcity360.model.Complaint;
import com.smartcity360.model.ComplaintStatus;
import com.smartcity360.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ComplaintRepository complaintRepository;

    public AnalyticsResponse buildAnalytics() {
        List<Complaint> all = complaintRepository.findAll();

        long total = all.size();
        long resolved = all.stream().filter(c -> c.getStatus() == ComplaintStatus.RESOLVED
                || c.getStatus() == ComplaintStatus.VERIFIED).count();
        long inProgress = all.stream().filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS).count();
        long unassigned = all.stream().filter(c -> c.getStatus() == ComplaintStatus.SUBMITTED
                || c.getStatus() == ComplaintStatus.CLASSIFIED).count();

        double avgResolutionDays = all.stream()
                .filter(c -> (c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.VERIFIED)
                        && c.getCreatedAt() != null && c.getUpdatedAt() != null)
                .mapToLong(c -> Duration.between(c.getCreatedAt(), c.getUpdatedAt()).toHours())
                .average().orElse(0) / 24.0;

        Map<String, Long> byCategory = all.stream()
                .filter(c -> c.getCategory() != null)
                .collect(Collectors.groupingBy(Complaint::getCategory, LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> byDepartment = all.stream()
                .filter(c -> c.getDepartment() != null)
                .collect(Collectors.groupingBy(Complaint::getDepartment, LinkedHashMap::new, Collectors.counting()));

        List<AnalyticsResponse.MonthlyPoint> monthly = buildMonthlyTrend(all);

        return AnalyticsResponse.builder()
                .totalComplaints(total)
                .resolvedCount(resolved)
                .inProgressCount(inProgress)
                .unassignedCount(unassigned)
                .avgResolutionDays(Math.round(avgResolutionDays * 10.0) / 10.0)
                .complaintsByCategory(byCategory)
                .complaintsByDepartment(byDepartment)
                .monthlyTrend(monthly)
                .build();
    }

    private List<AnalyticsResponse.MonthlyPoint> buildMonthlyTrend(List<Complaint> all) {
                Map<YearMonth, long[]> byMonth = new TreeMap<>(); // month -> [filed, resolved]

        for (Complaint c : all) {
                        if (c.getCreatedAt() == null) continue;
                        YearMonth month = YearMonth.from(c.getCreatedAt());
                        byMonth.computeIfAbsent(month, m -> new long[2])[0]++;
            if (c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.VERIFIED) {
                byMonth.get(month)[1]++;
            }
        }

        List<AnalyticsResponse.MonthlyPoint> result = new ArrayList<>();
                for (Map.Entry<YearMonth, long[]> e : byMonth.entrySet()) {
            result.add(AnalyticsResponse.MonthlyPoint.builder()
                    .month(e.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                            + " " + e.getKey().getYear())
                    .filed(e.getValue()[0]).resolved(e.getValue()[1]).build());
        }
        return result;
    }
}

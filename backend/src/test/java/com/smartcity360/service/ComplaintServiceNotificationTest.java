package com.smartcity360.service;

import com.smartcity360.dto.AssignRequest;
import com.smartcity360.dto.ComplaintCreateRequest;
import com.smartcity360.dto.ComplaintResponse;
import com.smartcity360.dto.StatusUpdateRequest;
import com.smartcity360.model.*;
import com.smartcity360.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ComplaintServiceNotificationTest {

    private ComplaintRepository complaintRepository;
    private AssignmentRepository assignmentRepository;
    private ResolutionRepository resolutionRepository;
    private FeedbackRepository feedbackRepository;
    private UserRepository userRepository;
    private AuditLogRepository auditLogRepository;
        private ComplaintHistoryRepository complaintHistoryRepository;
    private ClassificationService classificationService;
    private NotificationService notificationService;
    private ComplaintService complaintService;

    private User citizen;
    private User officer;
    private User admin;

    @BeforeEach
    void setUp() {
        complaintRepository = mock(ComplaintRepository.class);
        assignmentRepository = mock(AssignmentRepository.class);
        resolutionRepository = mock(ResolutionRepository.class);
        feedbackRepository = mock(FeedbackRepository.class);
        userRepository = mock(UserRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        complaintHistoryRepository = mock(ComplaintHistoryRepository.class);
        classificationService = mock(ClassificationService.class);
        notificationService = mock(NotificationService.class);

        complaintService = new ComplaintService(
                complaintRepository,
                assignmentRepository,
                resolutionRepository,
                feedbackRepository,
                userRepository,
                auditLogRepository,
                complaintHistoryRepository,
                classificationService,
                notificationService
        );

        citizen = User.builder().id(1L).name("Priya").email("priya@example.com").role(Role.CITIZEN).build();
        officer = User.builder().id(2L).name("Officer Kumar").email("kumar@example.com").role(Role.OFFICER).department("Public Works").build();
        admin = User.builder().id(3L).name("Admin Rao").email("admin@example.com").role(Role.ADMIN).build();
    }

    @Test
    void createComplaintDispatchesNotificationToCitizen() {
        ComplaintCreateRequest req = new ComplaintCreateRequest();
        req.setDescription("Pothole on Main Rd");
        req.setLocation("Central Square");

        when(classificationService.classifyToMap(anyString())).thenReturn(Map.of(
                "category", "Road Damage",
                "department", "Public Works",
                "priority", "HIGH"
        ));

        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> {
            Complaint c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });

        ComplaintResponse response = complaintService.createComplaint(citizen, req);

        assertNotNull(response);
        assertEquals("SC-20010", response.getPublicId());
        verify(notificationService, times(1)).createNotification(
                eq(citizen),
                eq("Complaint Registered"),
                contains("SC-20010"),
                any(Complaint.class),
                eq(NotificationType.COMPLAINT_SUBMITTED)
        );
    }

        @Test
        void createComplaintSetsPrioritySlaAndInitialHistory() {
                ComplaintCreateRequest req = new ComplaintCreateRequest();
                req.setDescription("Pothole on Main Rd");
                when(classificationService.classifyToMap(anyString())).thenReturn(Map.of(
                                "category", "Road Damage",
                                "department", "Public Works",
                                "priority", "HIGH"
                ));
                when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> {
                        Complaint complaint = invocation.getArgument(0);
                        complaint.setId(12L);
                        return complaint;
                });

                ComplaintResponse response = complaintService.createComplaint(citizen, req);

                assertNotNull(response.getSlaDueAt());
                assertEquals(response.getCreatedAt().plusHours(24), response.getSlaDueAt());
                verify(complaintHistoryRepository).save(argThat(history ->
                                history.getComplaint().getId().equals(12L)
                                                && history.getStatus() == ComplaintStatus.CLASSIFIED
                                                && history.getActor().equals(citizen)));
        }

        @Test
        void openComplaintPastSlaDeadlineIsMarkedOverdue() {
                Complaint complaint = Complaint.builder()
                                .id(31L)
                                .citizen(citizen)
                                .status(ComplaintStatus.IN_PROGRESS)
                                .slaDueAt(java.time.LocalDateTime.now().minusHours(1))
                                .build();
                when(complaintRepository.findById(31L)).thenReturn(Optional.of(complaint));
                when(assignmentRepository.findByComplaint(complaint)).thenReturn(Optional.empty());

                assertTrue(complaintService.getById(31L, admin).isOverdue());

                complaint.setStatus(ComplaintStatus.RESOLVED);
                assertFalse(complaintService.getById(31L, admin).isOverdue());
        }

    @Test
    void createComplaintDispatchesNotificationToAdmins() {
        ComplaintCreateRequest req = new ComplaintCreateRequest();
        req.setDescription("Pothole on Main Rd");
        req.setLocation("Central Square");

        when(classificationService.classifyToMap(anyString())).thenReturn(Map.of(
                "category", "Road Damage",
                "department", "Public Works",
                "priority", "HIGH"
        ));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> {
            Complaint c = invocation.getArgument(0);
            c.setId(11L);
            return c;
        });

        complaintService.createComplaint(citizen, req);

        verify(notificationService).createNotification(
                eq(admin),
                eq("New Complaint Submitted"),
                contains("SC-20011"),
                any(Complaint.class),
                eq(NotificationType.SYSTEM_ALERT)
        );
    }

    @Test
    void assignOfficerDispatchesNotificationToOfficerAndCitizen() {
        Complaint complaint = Complaint.builder()
                .id(20L)
                .citizen(citizen)
                .category("Road Damage")
                .status(ComplaintStatus.CLASSIFIED)
                .build();

        when(complaintRepository.findById(20L)).thenReturn(Optional.of(complaint));
        when(userRepository.findById(2L)).thenReturn(Optional.of(officer));
        when(assignmentRepository.findByComplaint(complaint)).thenReturn(Optional.empty());
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(i -> i.getArgument(0));

        complaintService.assignOfficer(20L, 2L, admin);

        verify(complaintHistoryRepository).save(argThat(history ->
                history.getStatus() == ComplaintStatus.ASSIGNED
                        && history.getActor().equals(admin)
                        && history.getMessage().contains("Officer Kumar")));

        verify(notificationService, times(1)).createNotification(
                eq(officer),
                eq("New Complaint Assigned"),
                contains("SC-20020"),
                eq(complaint),
                eq(NotificationType.OFFICER_ASSIGNED)
        );

        verify(notificationService, times(1)).createNotification(
                eq(citizen),
                eq("Officer Assigned"),
                contains("Officer Kumar"),
                eq(complaint),
                eq(NotificationType.OFFICER_ASSIGNED)
        );
    }

    @Test
    void updateStatusDispatchesNotificationToCitizen() {
        Complaint complaint = Complaint.builder()
                .id(30L)
                .citizen(citizen)
                .status(ComplaintStatus.IN_PROGRESS)
                .build();

        when(complaintRepository.findById(30L)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(i -> i.getArgument(0));

        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus(ComplaintStatus.RESOLVED);
        req.setResolutionNote("Road repaired successfully.");

        when(resolutionRepository.findByComplaint(complaint)).thenReturn(Optional.empty());

        complaintService.updateStatus(30L, req, admin);

        verify(complaintHistoryRepository).save(argThat(history ->
                history.getStatus() == ComplaintStatus.RESOLVED
                        && history.getActor().equals(admin)
                        && history.getMessage().contains("Road repaired successfully.")));

        verify(notificationService, times(1)).createNotification(
                eq(citizen),
                contains("Status Updated: RESOLVED"),
                contains("SC-20030"),
                eq(complaint),
                eq(NotificationType.STATUS_UPDATED)
        );
    }
}

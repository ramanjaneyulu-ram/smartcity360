package com.smartcity360.service;

import com.smartcity360.dto.StatusUpdateRequest;
import com.smartcity360.model.Assignment;
import com.smartcity360.model.Complaint;
import com.smartcity360.model.ComplaintStatus;
import com.smartcity360.model.Role;
import com.smartcity360.model.User;
import com.smartcity360.repository.AssignmentRepository;
import com.smartcity360.repository.AuditLogRepository;
import com.smartcity360.repository.ComplaintHistoryRepository;
import com.smartcity360.repository.ComplaintRepository;
import com.smartcity360.repository.FeedbackRepository;
import com.smartcity360.repository.ResolutionRepository;
import com.smartcity360.repository.UserRepository;
import com.smartcity360.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComplaintAuthorizationTest {

    @Test
    void citizenCannotReadAnotherCitizensComplaint() {
        ComplaintRepository complaintRepository = mock(ComplaintRepository.class);
        AssignmentRepository assignmentRepository = mock(AssignmentRepository.class);
        ComplaintService service = service(complaintRepository, assignmentRepository);
        User owner = user(1L, Role.CITIZEN);
        Complaint complaint = complaint(owner);
        when(complaintRepository.findById(12L)).thenReturn(Optional.of(complaint));

        assertThrows(AccessDeniedException.class, () -> service.getById(12L, user(2L, Role.CITIZEN)));
    }

    @Test
    void citizenCannotReadAnotherCitizensComplaintHistory() {
        ComplaintRepository complaintRepository = mock(ComplaintRepository.class);
        AssignmentRepository assignmentRepository = mock(AssignmentRepository.class);
        ComplaintHistoryRepository historyRepository = mock(ComplaintHistoryRepository.class);
        ComplaintService service = service(complaintRepository, assignmentRepository, historyRepository);
        when(complaintRepository.findById(12L)).thenReturn(Optional.of(complaint(user(1L, Role.CITIZEN))));

        assertThrows(AccessDeniedException.class, () -> service.getHistory(12L, user(2L, Role.CITIZEN)));
        verify(historyRepository, never()).findByComplaint_IdOrderByCreatedAtAsc(12L);
    }

    @Test
    void officerCannotUpdateComplaintAssignedToSomeoneElse() {
        ComplaintRepository complaintRepository = mock(ComplaintRepository.class);
        AssignmentRepository assignmentRepository = mock(AssignmentRepository.class);
        ComplaintService service = service(complaintRepository, assignmentRepository);
        Complaint complaint = complaint(user(1L, Role.CITIZEN));
        User officer = user(8L, Role.OFFICER);
        when(complaintRepository.findById(12L)).thenReturn(Optional.of(complaint));
        when(assignmentRepository.findByComplaint(complaint)).thenReturn(Optional.empty());
        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(ComplaintStatus.RESOLVED);

        assertThrows(AccessDeniedException.class, () -> service.updateStatus(12L, request, officer));
        verify(complaintRepository, never()).save(any(Complaint.class));
    }

    @Test
    void citizenCanReadTheirOwnComplaint() {
        ComplaintRepository complaintRepository = mock(ComplaintRepository.class);
        AssignmentRepository assignmentRepository = mock(AssignmentRepository.class);
        ComplaintService service = service(complaintRepository, assignmentRepository);
        Complaint complaint = complaint(user(1L, Role.CITIZEN));
        when(complaintRepository.findById(12L)).thenReturn(Optional.of(complaint));
        when(assignmentRepository.findByComplaint(complaint)).thenReturn(Optional.empty());

        assertEquals("Street flooding", service.getById(12L, user(1L, Role.CITIZEN)).getDescription());
    }

    private ComplaintService service(ComplaintRepository complaintRepository, AssignmentRepository assignmentRepository) {
        return service(complaintRepository, assignmentRepository, mock(ComplaintHistoryRepository.class));
    }

    private ComplaintService service(ComplaintRepository complaintRepository, AssignmentRepository assignmentRepository,
                                     ComplaintHistoryRepository historyRepository) {
        return new ComplaintService(complaintRepository, assignmentRepository,
                mock(ResolutionRepository.class), mock(FeedbackRepository.class), mock(UserRepository.class),
                mock(AuditLogRepository.class), historyRepository,
            mock(ClassificationService.class), mock(NotificationService.class));
    }

    private User user(Long id, Role role) {
        return User.builder().id(id).name("Test User").email("user@example.com").role(role).build();
    }

    private Complaint complaint(User citizen) {
        return Complaint.builder().id(12L).citizen(citizen).description("Street flooding")
                .status(ComplaintStatus.CLASSIFIED).build();
    }
}
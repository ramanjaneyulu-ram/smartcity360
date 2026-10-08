package com.smartcity360.service;

import com.smartcity360.dto.*;
import com.smartcity360.model.*;
import com.smartcity360.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final AssignmentRepository assignmentRepository;
    private final ResolutionRepository resolutionRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final ComplaintHistoryRepository complaintHistoryRepository;
    private final ClassificationService classificationService;
    private final NotificationService notificationService;

    public ComplaintResponse createComplaint(User citizen, ComplaintCreateRequest req) {
        Map<String, String> classification = classificationService.classifyToMap(req.getDescription());

        Priority priority = Priority.valueOf(classification.get("priority"));
        LocalDateTime submittedAt = LocalDateTime.now();
        Complaint complaint = Complaint.builder()
                .citizen(citizen)
                .description(req.getDescription())
                .location(req.getLocation())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .photoUrl(req.getPhotoUrl())
                .category(classification.get("category"))
                .department(classification.get("department"))
                .priority(priority)
                .status(ComplaintStatus.CLASSIFIED)
                .createdAt(submittedAt)
                .slaDueAt(submittedAt.plusHours(slaHours(priority)))
                .build();

        final Complaint savedComplaint = complaintRepository.save(complaint);
        complaint = savedComplaint;
        audit(citizen, "COMPLAINT_SUBMITTED", "Complaint #" + savedComplaint.getId() + " submitted and auto-classified as "
                + savedComplaint.getCategory());
        addHistory(savedComplaint, ComplaintStatus.CLASSIFIED,
            "Report submitted and classified as " + savedComplaint.getCategory() + ".", citizen);

        final String publicId = "SC-" + (20000 + savedComplaint.getId());

        userRepository.findByRole(Role.ADMIN).forEach(admin ->
                notificationService.createNotification(
                        admin,
                        "New Complaint Submitted",
                        "A new citizen complaint (" + publicId + ") has been submitted for " + savedComplaint.getDepartment() + ".",
                        savedComplaint,
                        NotificationType.SYSTEM_ALERT
                )
        );

        notificationService.createNotification(
                citizen,
                "Complaint Registered",
                "Your complaint (" + publicId + ") has been registered and classified as " + savedComplaint.getCategory() + " (" + savedComplaint.getDepartment() + ").",
                savedComplaint,
                NotificationType.COMPLAINT_SUBMITTED
        );

        return toResponse(savedComplaint);
    }

    public List<ComplaintResponse> getMyComplaints(User citizen) {
        return complaintRepository.findByCitizenOrderByCreatedAtDesc(citizen)
                .stream().map(this::toResponse).toList();
    }

    public ComplaintResponse getById(Long id, User requester) {
        return toResponse(getVisibleComplaint(id, requester));
    }

    public List<ComplaintHistoryResponse> getHistory(Long id, User requester) {
        getVisibleComplaint(id, requester);
        return complaintHistoryRepository.findByComplaint_IdOrderByCreatedAtAsc(id).stream()
                .map(entry -> ComplaintHistoryResponse.builder()
                        .status(entry.getStatus().name())
                        .message(entry.getMessage())
                        .actorName(entry.getActor() == null ? null : entry.getActor().getName())
                        .createdAt(entry.getCreatedAt())
                        .build())
                .toList();
    }

    public void authorizePhotoAccess(String photoUrl, User requester) {
        Complaint complaint = complaintRepository.findByPhotoUrl(photoUrl)
                .orElseThrow(() -> new AccessDeniedException("You do not have access to this image."));
        getVisibleComplaint(complaint, requester);
    }

    public List<ComplaintResponse> getQueueForDepartment(String department) {
        List<Complaint> list = department == null || department.isBlank()
                ? complaintRepository.findAllByOrderByCreatedAtDesc()
                : complaintRepository.findByDepartmentOrderByCreatedAtDesc(department);
        return list.stream().map(this::toResponse).toList();
    }

    public List<ComplaintResponse> getQueueForUser(User user, String department) {
        if (user.getRole() == Role.ADMIN) {
            return getQueueForDepartment(department);
        }
        return assignmentRepository.findComplaintsByOfficer(user).stream().map(this::toResponse).toList();
    }

    public ComplaintResponse assignOfficer(Long complaintId, Long officerId, User actor) {
        Complaint complaint = getEntity(complaintId);
        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> new IllegalArgumentException("Officer not found"));
        if (officer.getRole() != Role.OFFICER) {
            throw new IllegalArgumentException("Complaints can only be assigned to officer accounts.");
        }

        Assignment assignment = assignmentRepository.findByComplaint(complaint)
                .orElse(Assignment.builder().complaint(complaint).build());
        assignment.setOfficer(officer);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignmentRepository.save(assignment);

        complaint.setStatus(ComplaintStatus.ASSIGNED);
        complaint.setUpdatedAt(LocalDateTime.now());
        complaintRepository.save(complaint);

        audit(actor, "COMPLAINT_ASSIGNED", "Complaint #" + complaintId + " assigned to " + officer.getName());
        addHistory(complaint, ComplaintStatus.ASSIGNED, "Assigned to " + officer.getName() + ".", actor);

        String publicId = "SC-" + (20000 + complaintId);
        notificationService.createNotification(
                officer,
                "New Complaint Assigned",
                "You have been assigned to handle complaint " + publicId + " (" + complaint.getCategory() + ").",
                complaint,
                NotificationType.OFFICER_ASSIGNED
        );
        notificationService.createNotification(
                complaint.getCitizen(),
                "Officer Assigned",
                "Officer " + officer.getName() + " has been assigned to your complaint " + publicId + ".",
                complaint,
                NotificationType.OFFICER_ASSIGNED
        );

        return toResponse(complaint);
    }

    public ComplaintResponse updateStatus(Long complaintId, StatusUpdateRequest req, User actor) {
        Complaint complaint = getEntity(complaintId);
        if (actor.getRole() != Role.ADMIN && !isAssignedOfficer(complaint, actor)) {
            throw new AccessDeniedException("Only the assigned officer or an admin can update this complaint.");
        }
        complaint.setStatus(req.getStatus());
        complaint.setUpdatedAt(LocalDateTime.now());
        complaintRepository.save(complaint);

        if (req.getStatus() == ComplaintStatus.RESOLVED) {
            Resolution resolution = resolutionRepository.findByComplaint(complaint)
                    .orElse(Resolution.builder().complaint(complaint).build());
            resolution.setDescription(req.getResolutionNote());
            resolution.setEvidenceUrl(req.getEvidenceUrl());
            resolution.setResolvedAt(LocalDateTime.now());
            resolutionRepository.save(resolution);
        }

        audit(actor, "STATUS_UPDATED", "Complaint #" + complaintId + " status set to " + req.getStatus());
        String historyMessage = "Status changed to " + req.getStatus().name().replace('_', ' ').toLowerCase() + ".";
        if (req.getResolutionNote() != null && !req.getResolutionNote().isBlank()) {
            historyMessage += " " + req.getResolutionNote().trim();
        }
        addHistory(complaint, req.getStatus(), historyMessage, actor);

        String publicId = "SC-" + (20000 + complaintId);
        String note = req.getStatus() == ComplaintStatus.RESOLVED
                ? " The issue has been marked resolved. Please verify and leave feedback."
                : "";
        notificationService.createNotification(
                complaint.getCitizen(),
                "Status Updated: " + req.getStatus(),
                "Your complaint " + publicId + " is now " + req.getStatus() + "." + note,
                complaint,
                NotificationType.STATUS_UPDATED
        );

        return toResponse(complaint);
    }

    public ComplaintResponse submitFeedback(Long complaintId, FeedbackRequest req, User citizen) {
        Complaint complaint = getEntity(complaintId);
        if (citizen.getRole() != Role.CITIZEN || !complaint.getCitizen().getId().equals(citizen.getId())) {
            throw new AccessDeniedException("You can only submit feedback for your own complaint.");
        }
        Feedback feedback = feedbackRepository.findByComplaint(complaint)
                .orElse(Feedback.builder().complaint(complaint).build());
        feedback.setRating(req.getRating());
        feedback.setComment(req.getComment());
        feedbackRepository.save(feedback);

        complaint.setStatus(ComplaintStatus.VERIFIED);
        complaint.setUpdatedAt(LocalDateTime.now());
        complaintRepository.save(complaint);

        audit(citizen, "FEEDBACK_SUBMITTED", "Feedback recorded for complaint #" + complaintId);
        addHistory(complaint, ComplaintStatus.VERIFIED, "Citizen confirmed the resolution.", citizen);

        String publicId = "SC-" + (20000 + complaintId);
        assignmentRepository.findByComplaint(complaint).ifPresent(assignment -> {
            notificationService.createNotification(
                    assignment.getOfficer(),
                    "Citizen Feedback Received",
                    citizen.getName() + " submitted a " + req.getRating() + "/5 rating on complaint " + publicId + ".",
                    complaint,
                    NotificationType.FEEDBACK_RECEIVED
            );
        });

        return toResponse(complaint);
    }

    private Complaint getEntity(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found: " + id));
    }

    private Complaint getVisibleComplaint(Long id, User requester) {
        return getVisibleComplaint(getEntity(id), requester);
    }

    private Complaint getVisibleComplaint(Complaint complaint, User requester) {
        boolean isCitizenOwner = requester.getRole() == Role.CITIZEN
                && complaint.getCitizen().getId().equals(requester.getId());
        if (requester.getRole() != Role.ADMIN && !isCitizenOwner && !isAssignedOfficer(complaint, requester)) {
            throw new AccessDeniedException("You do not have access to this complaint.");
        }
        return complaint;
    }

    private boolean isAssignedOfficer(Complaint complaint, User user) {
        return user.getRole() == Role.OFFICER && assignmentRepository.findByComplaint(complaint)
                .map(assignment -> assignment.getOfficer().getId().equals(user.getId()))
                .orElse(false);
    }

    private void audit(User actor, String action, String details) {
        auditLogRepository.save(AuditLog.builder().actor(actor).action(action).details(details).build());
    }

    private void addHistory(Complaint complaint, ComplaintStatus status, String message, User actor) {
        complaintHistoryRepository.save(ComplaintHistory.builder()
                .complaint(complaint)
                .status(status)
                .message(message)
                .actor(actor)
                .build());
    }

    private long slaHours(Priority priority) {
        return switch (priority) {
            case HIGH -> 24;
            case MEDIUM -> 72;
            case LOW -> 168;
        };
    }

    private ComplaintResponse toResponse(Complaint c) {
        String officerName = assignmentRepository.findByComplaint(c)
                .map(a -> a.getOfficer().getName())
                .orElse(null);

        return ComplaintResponse.builder()
                .id(c.getId())
                .publicId("SC-" + (20000 + c.getId()))
                .description(c.getDescription())
                .category(c.getCategory())
                .department(c.getDepartment())
                .priority(c.getPriority() == null ? null : c.getPriority().name())
                .status(c.getStatus().name())
                .location(c.getLocation())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .photoUrl(c.getPhotoUrl())
                .citizenName(c.getCitizen().getName())
                .officerName(officerName)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .slaDueAt(c.getSlaDueAt())
                .overdue(c.getSlaDueAt() != null
                    && c.getSlaDueAt().isBefore(LocalDateTime.now())
                    && c.getStatus() != ComplaintStatus.RESOLVED
                    && c.getStatus() != ComplaintStatus.VERIFIED)
                .build();
    }
}

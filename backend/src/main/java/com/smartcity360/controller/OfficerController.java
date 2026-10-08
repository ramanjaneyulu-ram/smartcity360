package com.smartcity360.controller;

import com.smartcity360.dto.*;
import com.smartcity360.security.UserDetailsImpl;
import com.smartcity360.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/officer")
@RequiredArgsConstructor
public class OfficerController {

    private final ComplaintService complaintService;

    /** Officers see their assignments; admins retain the department-wide queue. */
    @GetMapping("/queue")
    public List<ComplaintResponse> queue(@RequestParam(required = false) String department,
                                         @AuthenticationPrincipal UserDetailsImpl principal) {
        return complaintService.getQueueForUser(principal.getUser(), department);
    }

    @PostMapping("/complaints/{id}/assign")
    public ComplaintResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req,
                                     @AuthenticationPrincipal UserDetailsImpl principal) {
        return complaintService.assignOfficer(id, req.getOfficerId(), principal.getUser());
    }

    @PatchMapping("/complaints/{id}/status")
    public ComplaintResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest req,
                                           @AuthenticationPrincipal UserDetailsImpl principal) {
        return complaintService.updateStatus(id, req, principal.getUser());
    }
}

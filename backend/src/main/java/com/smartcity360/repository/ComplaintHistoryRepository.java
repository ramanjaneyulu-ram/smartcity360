package com.smartcity360.repository;

import com.smartcity360.model.ComplaintHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintHistoryRepository extends JpaRepository<ComplaintHistory, Long> {
    List<ComplaintHistory> findByComplaint_IdOrderByCreatedAtAsc(Long complaintId);
}
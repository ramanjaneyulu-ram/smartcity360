package com.smartcity360.repository;

import com.smartcity360.model.Complaint;
import com.smartcity360.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Optional<Feedback> findByComplaint(Complaint complaint);
}

package com.smartcity360.repository;

import com.smartcity360.model.Complaint;
import com.smartcity360.model.Resolution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResolutionRepository extends JpaRepository<Resolution, Long> {
    Optional<Resolution> findByComplaint(Complaint complaint);
}

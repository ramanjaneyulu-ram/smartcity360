package com.smartcity360.repository;

import com.smartcity360.model.Assignment;
import com.smartcity360.model.Complaint;
import com.smartcity360.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    Optional<Assignment> findByComplaint(Complaint complaint);
    List<Assignment> findByOfficer(User officer);

    @Query("select a.complaint from Assignment a where a.officer = :officer order by a.complaint.createdAt desc")
    List<Complaint> findComplaintsByOfficer(@Param("officer") User officer);
}

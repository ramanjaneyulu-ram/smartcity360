package com.smartcity360.repository;

import com.smartcity360.model.Complaint;
import com.smartcity360.model.ComplaintStatus;
import com.smartcity360.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByCitizenOrderByCreatedAtDesc(User citizen);
    List<Complaint> findByDepartmentOrderByCreatedAtDesc(String department);
    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);
    Optional<Complaint> findByPhotoUrl(String photoUrl);
    List<Complaint> findAllByOrderByCreatedAtDesc();
    long countByStatus(ComplaintStatus status);
    long countByCategory(String category);
}

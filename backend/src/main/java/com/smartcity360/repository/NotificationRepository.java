package com.smartcity360.repository;

import com.smartcity360.model.Notification;
import com.smartcity360.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.complaint WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.read = false")
    long countByUserIdAndReadFalse(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.id = :id AND n.user.id = :userId")
    Optional<Notification> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    void markAllAsReadForUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.complaint WHERE n.complaint.id = :complaintId ORDER BY n.createdAt DESC")
    List<Notification> findByComplaintIdOrderByCreatedAtDesc(@Param("complaintId") Long complaintId);

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    long countByUserAndReadFalse(User user);

    Optional<Notification> findByIdAndUser(Long id, User user);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user = :user AND n.read = false")
    void markAllAsReadForUser(@Param("user") User user);
}

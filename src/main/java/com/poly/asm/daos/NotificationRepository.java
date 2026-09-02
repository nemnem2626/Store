package com.poly.asm.daos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.poly.asm.entitys.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop10ByTargetRoleOrderByIdDesc(String targetRole);

    long countByTargetRoleAndIsReadFalse(String targetRole);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.targetRole = :targetRole")
    void markAllRead(@Param("targetRole") String targetRole);
}

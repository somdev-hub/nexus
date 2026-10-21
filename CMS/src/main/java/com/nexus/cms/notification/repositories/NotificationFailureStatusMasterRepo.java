package com.nexus.cms.notification.repositories;

import com.nexus.cms.notification.entities.NotificationFailureStatusMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationFailureStatusMasterRepo
        extends JpaRepository<NotificationFailureStatusMaster, Long> {

    Optional<NotificationFailureStatusMaster> findByNotificationFailureStatus(String notificationFailureStatus);

    List<NotificationFailureStatusMaster> findByIsActiveTrue();
}

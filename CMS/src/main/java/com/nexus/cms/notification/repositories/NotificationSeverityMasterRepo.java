package com.nexus.cms.notification.repositories;

import com.nexus.cms.notification.entities.NotificationSeverityMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationSeverityMasterRepo extends JpaRepository<NotificationSeverityMaster, Long> {

    Optional<NotificationSeverityMaster> findByNotificationSeverity(String notificationSeverity);

    List<NotificationSeverityMaster> findByIsActiveTrue();
}

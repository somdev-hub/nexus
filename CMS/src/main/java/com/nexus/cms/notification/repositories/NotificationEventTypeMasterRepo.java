package com.nexus.cms.notification.repositories;

import com.nexus.cms.notification.entities.NotificationEventTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationEventTypeMasterRepo extends JpaRepository<NotificationEventTypeMaster, Long> {

    Optional<NotificationEventTypeMaster> findByNotificationEventType(String notificationEventType);

    List<NotificationEventTypeMaster> findByIsActiveTrue();
}

package com.nexus.cms.notification.repositories;

import com.nexus.cms.notification.entities.NotificationPushTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationPushTypeMasterRepo extends JpaRepository<NotificationPushTypeMaster, Long> {

    Optional<NotificationPushTypeMaster> findByNotificationPushType(String notificationPushType);

    List<NotificationPushTypeMaster> findByIsActiveTrue();
}

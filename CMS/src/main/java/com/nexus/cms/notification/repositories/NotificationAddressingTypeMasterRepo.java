package com.nexus.cms.notification.repositories;

import com.nexus.cms.notification.entities.NotificationAddressingTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationAddressingTypeMasterRepo
        extends JpaRepository<NotificationAddressingTypeMaster, Long> {

    Optional<NotificationAddressingTypeMaster> findByNotificationAddressingType(String notificationAddressingType);

    List<NotificationAddressingTypeMaster> findByIsActiveTrue();
}

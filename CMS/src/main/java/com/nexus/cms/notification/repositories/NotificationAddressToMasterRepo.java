package com.nexus.cms.notification.repositories;

import com.nexus.cms.notification.entities.NotificationAddressToMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationAddressToMasterRepo extends JpaRepository<NotificationAddressToMaster, Long> {

    Optional<NotificationAddressToMaster> findByNotificationAddressTo(String notificationAddressTo);

    List<NotificationAddressToMaster> findByIsActiveTrue();
}

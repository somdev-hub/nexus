package com.nexus.cms.notification.repositories;

import com.nexus.cms.notification.entities.NotificationPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationPolicyRepo extends JpaRepository<NotificationPolicy, Long> {

    /**
     * Find policies for a given event type.
     */
    List<NotificationPolicy> findByEventTypeOrderByNewStatusAsc(String eventType);

    /**
     * Find active policies for a given event type.
     */
    List<NotificationPolicy> findByEventTypeAndIsActiveTrue(String eventType);

    /**
     * Find a policy by event type, previous status, new status, and address-to.
     */
    Optional<NotificationPolicy> findByEventTypeAndPreviousStatusAndNewStatusAndAddressTo(
            String eventType, String previousStatus, String newStatus, String addressTo);

    /**
     * Find all active policies.
     */
    List<NotificationPolicy> findByIsActiveTrue();

    /**
     * Paginated list of all policies.
     */
    Page<NotificationPolicy> findAllByOrderByEventTypeAscNewStatusAsc(Pageable pageable);

    /**
     * Find policies by event type (paginated).
     */
    Page<NotificationPolicy> findByEventTypeOrderByNewStatusAsc(String eventType, Pageable pageable);
}
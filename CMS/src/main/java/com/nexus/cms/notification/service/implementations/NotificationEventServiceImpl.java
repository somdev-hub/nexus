package com.nexus.cms.notification.service.implementations;

import com.nexus.cms.notification.entities.Notification;
import com.nexus.cms.notification.model.DomainEventDto;
import com.nexus.cms.notification.service.interfaces.NotificationDeliveryService;
import com.nexus.cms.notification.service.interfaces.NotificationEventService;
import com.nexus.cms.notification.service.interfaces.NotificationMasterDataService;
import com.nexus.cms.notification.service.interfaces.NotificationProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Turns a domain event into a notification and hands it for delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventServiceImpl implements NotificationEventService {

    private final NotificationProjectionService projectionService;
    private final NotificationDeliveryService deliveryService;
    private final NotificationMasterDataService masterData;

    @Override
    public void handle(DomainEventDto event) {
        if (event == null) {
            return;
        }

        // The event type catalogue lives in the database, so an event type can
        // be added or deactivated without a release. An event whose type is
        // not active is acknowledged and ignored — it is not a failure.
        if (!masterData.isActiveEventType(event.getEventType())) {
            log.info("Event type '{}' is not active — event {} is ignored",
                    event.getEventType(), event.getEventId());
            return;
        }

        Notification notification = projectionService.project(event);
        if (notification == null) {
            // Either the transition is not notifiable, or the event has been
            // projected already. Neither is a failure.
            return;
        }

        deliveryService.deliver(notification, event);
    }
}

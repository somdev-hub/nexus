package com.nexus.cms.notification.service.implementations;

import com.nexus.cms.notification.entities.Notification;
import com.nexus.cms.notification.properties.NotificationMasterCodes;
import com.nexus.cms.notification.model.DomainEventDto;
import com.nexus.cms.notification.service.interfaces.NotificationDeliveryRouter;
import com.nexus.cms.notification.service.interfaces.NotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes a notification to the router that matches its addressing mode.
 * <p>
 * The routers are collected at start-up, so adding user-level or
 * authority-level addressing later means adding one more
 * {@link NotificationDeliveryRouter} and nothing else — no change here, and
 * no change to the projection or the protocol.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryServiceImpl implements NotificationDeliveryService {

    private final List<NotificationDeliveryRouter> routers;

    private Map<String, NotificationDeliveryRouter> routersByAddressingType;

    @Override
    public void deliver(Notification notification, DomainEventDto event) {
        if (notification == null) {
            return;
        }
        NotificationDeliveryRouter router = routerFor(notification);
        if (router == null) {
            log.warn("No delivery router for notification {} — it is stored but not delivered",
                    notification.getNotificationId());
            return;
        }
        try {
            router.deliver(notification);
        } catch (Exception e) {
            // The notification is already committed. A delivery failure costs
            // the live push, not the notification.
            log.error("Failed to deliver notification {} — it is stored and will be picked up on the next sync",
                    notification.getNotificationId(), e);
        }
    }

    private NotificationDeliveryRouter routerFor(Notification notification) {
        // The addressing type is stored with the notification, so delivery
        // does not have to look the policy up again.
        String addressing = notification.getAddressing();
        if (addressing == null || addressing.isBlank()) {
            addressing = NotificationMasterCodes.ADDRESSING_ORG;
        }
        return routerFor(addressing);
    }

    private NotificationDeliveryRouter routerFor(String addressing) {
        if (routersByAddressingType == null) {
            Map<String, NotificationDeliveryRouter> index = new HashMap<>();
            for (NotificationDeliveryRouter router : routers) {
                index.put(router.addressingType(), router);
            }
            routersByAddressingType = index;
            log.info("Notification delivery routers registered: {}", index.keySet());
        }
        return routersByAddressingType.get(addressing);
    }
}

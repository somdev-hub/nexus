package com.nexus.cms.notification.service.implementations;

import com.nexus.cms.notification.entities.Notification;
import com.nexus.cms.notification.properties.NotificationMasterCodes;
import com.nexus.cms.notification.model.NotificationMessageDto;
import com.nexus.cms.notification.properties.NotificationConstants;
import com.nexus.cms.notification.properties.NotificationProperties;
import com.nexus.cms.notification.service.interfaces.NotificationDeliveryRouter;
import com.nexus.cms.notification.service.interfaces.NotificationProjectionService;
import com.nexus.cms.notification.service.interfaces.NotificationRedisPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Delivers a notification to every member of an organisation.
 * <p>
 * The notification is broadcast on the organisation topic
 * {@code /topic/nexus/notifications/org/{orgId}}. Because the STOMP broker
 * is in-memory and lives inside a single JVM, the broadcast has to reach
 * every instance: in the default {@code REDIS} relay mode the notification
 * is published to Redis and each instance relays it to the clients it holds.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrgTopicDeliveryRouter implements NotificationDeliveryRouter {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRedisPublisher redisPublisher;
    private final NotificationProperties properties;
    private final NotificationProjectionService projectionService;

    @Override
    public String addressingType() {
        return NotificationMasterCodes.ADDRESSING_ORG;
    }

    @Override
    public void deliver(Notification notification) {
        if (notification == null || notification.getTargetOrgId() == null) {
            log.warn("Cannot deliver a notification without an organisation");
            return;
        }
        NotificationMessageDto message = projectionService.toMessageDto(notification);

        if (properties.isRedisRelay()) {
            redisPublisher.publishToOrg(notification.getTargetOrgId(), message);
            return;
        }

        // Direct mode: push straight to the clients connected to this
        // instance. Only correct with a single CMS instance.
        String destination = NotificationConstants.ORG_TOPIC_PREFIX + notification.getTargetOrgId();
        try {
            messagingTemplate.convertAndSend(destination, message);
            log.debug("[NOTIFICATION DELIVERY] Broadcast directly — destination={}, notificationId={}",
                    destination, notification.getNotificationId());
        } catch (Exception e) {
            log.error("[NOTIFICATION DELIVERY] Failed to broadcast notification {} to org {}",
                    notification.getNotificationId(), notification.getTargetOrgId(), e);
        }
    }
}

package com.nexus.cms.notification.controller;

import com.nexus.cms.notification.model.NotificationAckDto;
import com.nexus.cms.notification.model.NotificationMarkAllReadRequestDto;
import com.nexus.cms.notification.model.NotificationMarkReadRequestDto;
import com.nexus.cms.notification.model.NotificationPushDto;
import com.nexus.cms.notification.model.NotificationSnapshotDto;
import com.nexus.cms.notification.properties.NotificationConstants;
import com.nexus.cms.notification.service.interfaces.NotificationProjectionService;
import com.nexus.cms.util.StompPrincipalNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * The client-to-server half of the notification protocol.
 * <p>
 * There is deliberately <b>no REST controller</b> for notifications. Listing
 * the inbox, marking something read and asking for the unread count are all
 * STOMP commands over the socket the browser already holds, so the server
 * pushes rather than being polled.
 * <p>
 * The browser subscribes to:
 * <ul>
 *   <li>{@code /topic/nexus/notifications/org/{orgId}} — live notifications
 *   for the organisation the user is acting in</li>
 *   <li>{@code /user/queue/nexus/notifications} — the snapshot and the read
 *   acknowledgements</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketController {

    private final NotificationProjectionService projectionService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send the current inbox and unread count to the requesting user.
     * <p>
     * The browser sends this straight after subscribing, so that anything
     * that arrived while it was disconnected is picked up.
     */
    @MessageMapping("/nexus/notifications/ws/sync")
    public void sync(Principal principal, @Payload(required = false) Object ignored) {
        Long userId = StompPrincipalNames.userIdOf(principal.getName());
        Long orgId = StompPrincipalNames.orgIdOf(principal.getName());
        if (userId == null || orgId == null) {
            log.warn("Cannot serve a notification snapshot for principal {}", principal.getName());
            return;
        }
        NotificationSnapshotDto snapshot = projectionService.snapshot(userId, orgId, 0);
        messagingTemplate.convertAndSendToUser(principal.getName(),
                NotificationConstants.USER_QUEUE_DESTINATION,
                NotificationPushDto.ofSnapshot(snapshot));
        log.debug("Sent notification snapshot to user {} — {} items, {} unread",
                userId, snapshot.getItems().size(), snapshot.getUnreadCount());
    }

    /**
     * Mark one notification as read.
     */
    @MessageMapping("/nexus/notifications/ws/mark-read")
    public void markRead(Principal principal, @Payload NotificationMarkReadRequestDto request) {
        Long userId = StompPrincipalNames.userIdOf(principal.getName());
        Long orgId = StompPrincipalNames.orgIdOf(principal.getName());
        if (userId == null || orgId == null || request == null || request.getNotificationId() == null) {
            return;
        }
        long unreadCount = projectionService.markRead(request.getNotificationId(), userId, orgId);
        NotificationAckDto ack = NotificationAckDto.builder()
                .notificationId(request.getNotificationId())
                .unreadCount(unreadCount)
                .build();
        messagingTemplate.convertAndSendToUser(principal.getName(),
                NotificationConstants.USER_QUEUE_DESTINATION,
                NotificationPushDto.ofAck(ack));
    }

    /**
     * Mark every unread notification as read.
     */
    @MessageMapping("/nexus/notifications/ws/mark-all-read")
    public void markAllRead(Principal principal,
                            @Payload(required = false) NotificationMarkAllReadRequestDto request) {
        Long userId = StompPrincipalNames.userIdOf(principal.getName());
        Long orgId = StompPrincipalNames.orgIdOf(principal.getName());
        if (userId == null || orgId == null) {
            return;
        }
        long unreadCount = projectionService.markAllRead(userId, orgId);
        NotificationAckDto ack = NotificationAckDto.builder()
                .notificationId(null)
                .unreadCount(unreadCount)
                .build();
        messagingTemplate.convertAndSendToUser(principal.getName(),
                NotificationConstants.USER_QUEUE_DESTINATION,
                NotificationPushDto.ofAck(ack));
    }
}

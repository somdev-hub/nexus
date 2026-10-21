package com.nexus.cms.notification.model;

import com.nexus.cms.notification.properties.NotificationMasterCodes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The single payload type carried on the private notification queue.
 * <p>
 * The browser keeps one subscription to
 * {@code /user/queue/nexus/notifications} and switches on {@link #type}.
 * Keeping everything on one destination means the browser never has to
 * manage several subscriptions, and the server never has to guess whether a
 * particular subscription is in place before it replies.
 * <p>
 * Live notifications are <b>not</b> sent here: they are broadcast to the
 * organisation topic, because at that point the server knows the
 * organisation but not the individual users.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPushDto {

    /**
     * Which of the payload fields is populated.
     * <p>
     * A code rather than an enum — the catalogue lives in
     * {@code cms.t_notification_push_type_master}.
     */
    private String type;

    /**
     * Populated when {@link #type} is {@code MESSAGE}.
     */
    private NotificationMessageDto message;

    /**
     * Populated when {@link #type} is {@code SNAPSHOT}.
     */
    private NotificationSnapshotDto snapshot;

    /**
     * Populated when {@link #type} is {@code ACK}.
     */
    private NotificationAckDto ack;

    public static NotificationPushDto ofMessage(NotificationMessageDto message) {
        return NotificationPushDto.builder()
                .type(NotificationMasterCodes.PUSH_TYPE_MESSAGE)
                .message(message)
                .build();
    }

    public static NotificationPushDto ofSnapshot(NotificationSnapshotDto snapshot) {
        return NotificationPushDto.builder()
                .type(NotificationMasterCodes.PUSH_TYPE_SNAPSHOT)
                .snapshot(snapshot)
                .build();
    }

    public static NotificationPushDto ofAck(NotificationAckDto ack) {
        return NotificationPushDto.builder()
                .type(NotificationMasterCodes.PUSH_TYPE_ACK)
                .ack(ack)
                .build();
    }
}

package com.nexus.cms.notification.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to mark every unread notification as read.
 * <p>
 * Carries no fields at the moment, but it is a distinct type so that the
 * destination stays explicit and a payload can be added later without
 * breaking the protocol.
 */
@Data
@NoArgsConstructor
public class NotificationMarkAllReadRequestDto {
}

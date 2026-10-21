package com.nexus.cms.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to mark one notification as read.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMarkReadRequestDto {

    /**
     * The notification to mark read.
     */
    private Long notificationId;
}

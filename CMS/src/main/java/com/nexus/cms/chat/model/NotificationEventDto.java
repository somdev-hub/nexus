package com.nexus.cms.chat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEventDto {
    private Long userId;
    private Long messageId;
    private Long conversationId;
    private String participantName;
    private String preview;
}

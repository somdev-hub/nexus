package com.nexus.cms.chat.model;

import com.nexus.cms.chat.enums.ChatMessageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusUpdateEventDto {
    private Long messageId;
    private ChatMessageStatus status;
}

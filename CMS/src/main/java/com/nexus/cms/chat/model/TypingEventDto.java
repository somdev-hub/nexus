package com.nexus.cms.chat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypingEventDto {
    private Long userId;
    private Boolean isTyping;
}

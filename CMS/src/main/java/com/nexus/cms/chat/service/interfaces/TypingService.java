package com.nexus.cms.chat.service.interfaces;

import java.util.List;

public interface TypingService {

    void setTyping(Long conversationId, Long participantId, boolean isTyping);

    List<Long> getTypingUsers(Long conversationId);
}

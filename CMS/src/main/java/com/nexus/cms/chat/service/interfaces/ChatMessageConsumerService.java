package com.nexus.cms.chat.service.interfaces;

public interface ChatMessageConsumerService {

    void broadcastMessage(String message);
}

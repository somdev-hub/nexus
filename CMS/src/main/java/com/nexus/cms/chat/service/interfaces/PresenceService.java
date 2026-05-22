package com.nexus.cms.chat.service.interfaces;

import com.nexus.cms.chat.model.PresenceEventDto;

public interface PresenceService {
    void setOnline(Long userId);
    void setOffline(Long userId);
    boolean isOnline(Long userId);
    void heartbeat(Long userId);
    void broadcast(Long userId, PresenceEventDto.PresenceEventStatus status);
}

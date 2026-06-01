package com.nexus.cms.chat.service.interfaces;

import com.nexus.cms.chat.model.PresenceEventDto;

import java.util.List;
import java.util.Map;

public interface PresenceService {
    void setOnline(Long userId);

    void setOffline(Long userId);

    boolean isOnline(Long userId);

    Map<Long, Boolean> getOnlineStatuses(List<Long> userIds);

    void heartbeat(Long userId);

    void broadcast(Long userId, PresenceEventDto.PresenceEventStatus status);
}

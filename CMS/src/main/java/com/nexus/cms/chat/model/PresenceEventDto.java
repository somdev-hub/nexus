package com.nexus.cms.chat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PresenceEventDto {
    private Long userId;
    private PresenceEventStatus status;
    private Timestamp at;

    public enum PresenceEventStatus {
        ONLINE,
        OFFLINE,
        AWAY,
        DO_NOT_DISTURB
    }
}

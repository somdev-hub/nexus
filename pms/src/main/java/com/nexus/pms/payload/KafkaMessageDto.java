package com.nexus.pms.payload;

import lombok.Data;

@Data
public class KafkaMessageDto {
    private String topic;
    private String commsType;
    private String uuid;
    private String message;
}

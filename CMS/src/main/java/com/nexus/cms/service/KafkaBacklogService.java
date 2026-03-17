package com.nexus.cms.service;

import com.nexus.cms.model.entities.KafkaBacklogs;
import com.nexus.cms.model.enums.KafkaStatus;
import com.nexus.cms.repository.KafkaBacklogsRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaBacklogService {

    private KafkaBacklogsRepo kafkaBacklogsRepo;

    public void logReceived(String topic, String uuid) {
        try {
            KafkaBacklogs kafkaBacklog = new KafkaBacklogs();
            kafkaBacklog.setTopic(topic);
            kafkaBacklog.setUuid(uuid);
            kafkaBacklog.setStatus(KafkaStatus.RECEIVED);
            kafkaBacklog.setMessageReceivedAt(new Timestamp(System.currentTimeMillis()));
            kafkaBacklog.setHasProcessed(Boolean.FALSE);
            kafkaBacklogsRepo.save(kafkaBacklog);
        } catch (Exception e) {
            log.error("Error logging Kafka message reception for topic: {} and uuid: {}. Error: {}", topic, uuid, e.getMessage());
        }
    }
}

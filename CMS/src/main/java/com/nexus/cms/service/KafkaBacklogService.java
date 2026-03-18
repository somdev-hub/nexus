package com.nexus.cms.service;

import com.nexus.cms.model.entities.KafkaBacklogs;
import com.nexus.cms.model.enums.KafkaStatus;
import com.nexus.cms.repository.KafkaBacklogsRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaBacklogService {

    private final KafkaBacklogsRepo kafkaBacklogsRepo;

    public void logReceived(String topic, String uuid) {
        try {
            KafkaBacklogs kafkaBacklog = new KafkaBacklogs();
            kafkaBacklog.setTopic(topic);
            kafkaBacklog.setUuid(uuid);
            kafkaBacklog.setStatus(KafkaStatus.RECEIVED);
            kafkaBacklog.setMessageReceivedAt(new Timestamp(System.currentTimeMillis()));
            kafkaBacklog.setHasProcessed(Boolean.FALSE);
            kafkaBacklogsRepo.save(kafkaBacklog);
        } catch (RuntimeException e) {
            log.error("Error logging Kafka message reception for topic: {} and uuid: {}. Error: {}", topic, uuid, e.getMessage());
        }
    }

    public void logProcessed(String topic, String uuid) {
        try {
            Optional<KafkaBacklogs> byUuid = kafkaBacklogsRepo.findByUuid(uuid);
            if (byUuid.isPresent()) {
                KafkaBacklogs kafkaBacklog = byUuid.get();
                kafkaBacklog.setStatus(KafkaStatus.PROCESSED);
                kafkaBacklog.setProcessedAt(new Timestamp(System.currentTimeMillis()));
                kafkaBacklog.setHasProcessed(Boolean.TRUE);
                kafkaBacklogsRepo.save(kafkaBacklog);
            } else {
                log.warn("Kafka backlog entry not found for uuid: {}. Unable to log processing status.", uuid);
            }
        } catch (RuntimeException e) {
            log.error("Error logging Kafka message processing for topic: {} and uuid: {}. Error: {}", topic, uuid, e.getMessage());
        }
    }

    public void logFailed(String topic, String uuid) {
        try {
            Optional<KafkaBacklogs> byUuid = kafkaBacklogsRepo.findByUuid(uuid);
            if (byUuid.isPresent()) {
                KafkaBacklogs kafkaBacklog = byUuid.get();
                kafkaBacklog.setStatus(KafkaStatus.FAILED);
                kafkaBacklog.setProcessedAt(new Timestamp(System.currentTimeMillis()));
                kafkaBacklog.setHasProcessed(Boolean.FALSE);
                kafkaBacklogsRepo.save(kafkaBacklog);
            } else {
                log.warn("Kafka backlog entry not found for uuid: {}. Unable to log failure status.", uuid);
            }
        } catch (RuntimeException e) {
            log.error("Error logging Kafka message failure for topic: {} and uuid: {}. Error: {}", topic, uuid, e.getMessage());
        }
    }
}

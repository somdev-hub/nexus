package com.nexus.cms.model.entities;

import com.nexus.cms.model.enums.KafkaStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Entity
@Table(name = "t_kafka_backlogs", schema = "cms")
@Data
public class KafkaBacklogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long kafkaBacklogsId;

    private Timestamp messageReceivedAt;

    private String topic;

    private Long orgId;

    private String templateParam;

    @Column(nullable = false, unique = true)
    private String uuid;

    @Enumerated(EnumType.STRING)
    private KafkaStatus status;

    private Boolean hasProcessed;

    private Timestamp processedAt;
}

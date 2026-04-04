package com.nexus.pms.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Entity for storing idempotency records.
 * Links an idempotency key to a payment ID to prevent duplicate processing.
 */
@Entity
@Table(name = "idempotency_records", schema = "pms")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 255)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false)
    private Timestamp createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Timestamp(System.currentTimeMillis());
    }
}

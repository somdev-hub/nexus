package com.nexus.pms.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;

@Entity
@Data
@Table(name = "t_pms_logs", schema = "pms")
public class PmsLogs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pmsLogsid;

    private String requestUrl;

    private String httpMethod;

    private int responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String request;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String response;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    private Timestamp createdOn = new Timestamp(System.currentTimeMillis());
}

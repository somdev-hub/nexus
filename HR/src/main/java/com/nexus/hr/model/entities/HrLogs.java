package com.nexus.hr.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;

@Entity
@Data
@Table(name = "t_hr_logs", schema = "hr")
public class HrLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestUrl;

    private String httpMethod;

    private int responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String request;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String response;

    private Long hrId;

    private Timestamp createdOn = new Timestamp(System.currentTimeMillis());
}

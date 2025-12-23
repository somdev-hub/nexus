package com.nexus.core.entities;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "t_logs", schema = "core")
public class Logs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestUrl;

    private String httpMethod;

    private int responseStatus;

    @Column(columnDefinition = "jsonb")
    private Object request;

    @Column(columnDefinition = "jsonb")
    private Object response;

    private Long org;

    private Timestamp createdOn= new Timestamp(System.currentTimeMillis());

}

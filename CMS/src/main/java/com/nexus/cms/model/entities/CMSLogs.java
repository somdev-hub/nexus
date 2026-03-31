package com.nexus.cms.model.entities;

import com.nexus.cms.model.enums.CommsStatus;
import com.nexus.cms.model.enums.CommsType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "t_cms_logs", schema = "cms")
@Data
public class CMSLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cmsLogsId;

    private String requestService;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> recipientEmails;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> ccEmails;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> bccEmails;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> phoneNumbers;

    @Enumerated(EnumType.STRING)
    private CommsType commsType;

    private String message;

    @Column(nullable = false, unique = true)
    private String uuid;

    private Timestamp createdAt;

    @Enumerated(EnumType.STRING)
    private CommsStatus status;
}

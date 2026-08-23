package com.nexus.nexusbuddy.model.entities;

import com.nexus.nexusbuddy.model.enums.DataType;
import com.nexus.nexusbuddy.model.enums.ParamType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;

@Entity
@Table(name = "t_tools_param_config", schema = "nexusbuddy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "toolsConfig" })
@EqualsAndHashCode(exclude = { "toolsConfig" })
public class ToolsParamConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long toolsParamConfigId;

    private String paramName;

    @Enumerated(EnumType.STRING)
    private ParamType paramType;

    @Enumerated(EnumType.STRING)
    private DataType dataType;

    private Boolean isRequired;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private Object defaultValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private Object requestBodyJson;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        isActive = true;
    }

    @ManyToOne
    @JoinColumn(name = "tools_config_tools_config_id")
    @JsonBackReference
    private ToolsConfig toolsConfig;
}

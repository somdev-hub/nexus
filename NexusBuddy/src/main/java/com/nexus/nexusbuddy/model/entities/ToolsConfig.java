package com.nexus.nexusbuddy.model.entities;

import com.nexus.nexusbuddy.model.enums.ToolsHttpMethod;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "t_tools_config", schema = "nexusbuddy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"clientConfig", "toolsParamConfigList"})
@EqualsAndHashCode(exclude = {"clientConfig", "toolsParamConfigList"})
public class ToolsConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long toolsConfigId;

    private String toolName;

    @Column(columnDefinition = "TEXT")
    private String toolDescription;

    private String endpoint;

    @Enumerated(EnumType.STRING)
    private ToolsHttpMethod httpMethod;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        isActive = true;
    }

    @OneToMany(mappedBy = "toolsConfig", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ToolsParamConfig> toolsParamConfigList;

    @ManyToOne
    @JoinColumn(name = "client_config_client_config_id")
    @JsonBackReference
    private ClientConfig clientConfig;
}

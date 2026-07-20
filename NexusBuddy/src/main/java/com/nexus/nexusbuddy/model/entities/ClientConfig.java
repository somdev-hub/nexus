package com.nexus.nexusbuddy.model.entities;

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
@Table(name = "t_client_config", schema = "nexusbuddy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"toolsConfigList"})
@ToString(exclude = {"toolsConfigList"})
public class ClientConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "client_config_id")
        private Long clientConfigId;

    private String clientName;

    private String clientDescription;

    private String connectionUrl;

    private String healthCheckPath;

    @CreationTimestamp
    private Timestamp createdOn;

    @UpdateTimestamp
    private Timestamp updatedOn;

    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        isActive = true;
    }

    @OneToMany(mappedBy = "clientConfig", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ToolsConfig> toolsConfigList;
}

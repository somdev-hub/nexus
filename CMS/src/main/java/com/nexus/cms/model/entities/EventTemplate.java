package com.nexus.cms.model.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nexus.cms.model.enums.EventTemplateType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "t_event_template", schema = "cms")
@Data
public class EventTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventTemplateId;

    private String templateName;

    @Enumerated(EnumType.STRING)
    private EventTemplateType eventTemplateType;

    private String templateHtmlUrl;

    private Long orgId;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonManagedReference(value = "eventTemplate-templateParams")
    @OneToMany(orphanRemoval = true, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<TemplateParam> templateParams;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        this.isActive = true;
    }

    @Transient
    private String templateHtml;
}

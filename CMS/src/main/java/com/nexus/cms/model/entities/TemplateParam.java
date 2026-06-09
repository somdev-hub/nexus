package com.nexus.cms.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.nexus.cms.model.enums.TemplateParamType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Table(name = "t_template_params", schema = "cms")
@Entity
@Data
public class TemplateParam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long templateParamId;

    private String paramName;

    private String paramDefaultValue;

    @Enumerated(EnumType.STRING)
    private TemplateParamType templateParamType;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonBackReference(value = "eventTemplate-templateParams")
    @ManyToOne
    @JoinColumn(name = "event_template_event_template_id")
    private EventTemplate eventTemplate;

    private Boolean isRequired;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    private Boolean isActive;

    @PrePersist
    protected void onCreate(){
        this.isActive=true;
    }
}

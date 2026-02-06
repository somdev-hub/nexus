package com.nexus.iam.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "t_resources", schema = "iam")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resourceId;

    @Column(unique = true, nullable = false)
    private String resourceName;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType resourceType; // e.g., "DOCUMENT", "REPORT", "MODULE", "API_ENDPOINT"
}

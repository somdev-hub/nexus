package com.nexus.iam.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "t_permissions", schema = "iam", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "role_id", "resource_id", "action" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long permissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PermissionAction action; // e.g., "READ", "CREATE", "UPDATE", "DELETE"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department; // Optional - if null, applies to all departments
}

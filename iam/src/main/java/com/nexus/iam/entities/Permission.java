package com.nexus.iam.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "t_permissions", schema = "iam", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"role_id", "resource_id", "department_id"})
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

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "t_permission_actions", joinColumns = @JoinColumn(name = "permission_id"))
    @Column(name = "action")
    @Enumerated(EnumType.STRING)
    private Set<PermissionAction> actions; // e.g., "READ", "CREATE", "UPDATE", "DELETE"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department; // Optional - if null, applies to all departments
}

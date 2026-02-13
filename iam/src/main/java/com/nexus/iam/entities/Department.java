package com.nexus.iam.entities;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@Table(name = "t_departments", schema = "iam")
@NoArgsConstructor
@AllArgsConstructor
@lombok.ToString(exclude = {"departmentHead", "members", "organization"})
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long departmentId;

    private String departmentName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_head_id")
    @JsonBackReference(value = "department-head")
    private User departmentHead;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "t_department_members",
        schema = "iam",
        joinColumns = @JoinColumn(name = "department_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> members = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "t_department_roles",
        schema = "iam",
        joinColumns = @JoinColumn(name = "department_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "org_id")
    @JsonBackReference(value = "organization-departments")
    private Organization organization;

    private Timestamp createdAt;

    // src/main/java/com/nexus/iam/entities/Department.java
    public void addMember(User user) {
        if (!this.members.contains(user)) {
            this.members.add(user);
        }
        if (user.getMemberOfDepartments() == null) {
            user.setMemberOfDepartments(new java.util.ArrayList<>());
        }
        if (!user.getMemberOfDepartments().contains(this)) {
            user.getMemberOfDepartments().add(this);
        }
    }

    public void addDepartmentHead(User user) {
        if (this.departmentHead == null || !this.departmentHead.equals(user)) {
            this.departmentHead=user;
        }
        if (user.getHeadedDepartments() == null) {
            user.setHeadedDepartments(new java.util.ArrayList<>());
        }
        if (!user.getHeadedDepartments().contains(this)) {
            user.getHeadedDepartments().add(this);
        }
    }

    public void removeMember(User user) {
        if (this.members.remove(user)) {
            if (user.getMemberOfDepartments() != null) {
                user.getMemberOfDepartments().remove(this);
            }
        }
    }
}

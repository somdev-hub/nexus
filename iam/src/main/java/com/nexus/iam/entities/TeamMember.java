package com.nexus.iam.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nexus.iam.entities.enums.TeamRole;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "t_team_members", schema = "iam")
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "team", "user", "manager", "subordinates" })
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    @JsonBackReference(value = "team-members")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference(value = "user-team-memberships")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    @JsonBackReference(value = "member-subordinates")
    private TeamMember manager;

    @OneToMany(mappedBy = "manager", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "member-subordinates")
    private List<TeamMember> subordinates = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private TeamRole role;

    @Column(name = "hierarchy_level", nullable = false)
    private Integer hierarchyLevel = 0;

    @Column(name = "team_position")
    private String teamPosition;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false)
    private Timestamp assignedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public void addSubordinate(TeamMember subordinate) {
        if (!this.subordinates.contains(subordinate)) {
            this.subordinates.add(subordinate);
            subordinate.setManager(this);
            subordinate.setHierarchyLevel(this.hierarchyLevel + 1);
        }
    }

    public void removeSubordinate(TeamMember subordinate) {
        if (this.subordinates.remove(subordinate)) {
            subordinate.setManager(null);
            subordinate.setHierarchyLevel(0);
        }
    }

    // Helper to check if this member is the team lead
    public boolean isTeamLead() {
        return this.role == TeamRole.TEAM_LEAD && this.hierarchyLevel == 0;
    }

    // Helper to check if this member is a manager (not team lead)
    public boolean isManager() {
        return this.role == TeamRole.MANAGER && this.hierarchyLevel > 0;
    }

    // Helper to check if this member is an employee (leaf node)
    public boolean isEmployee() {
        return this.role == TeamRole.EMPLOYEE;
    }
}
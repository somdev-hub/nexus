package com.nexus.iam.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
@Table(name = "t_teams", schema = "iam")
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "department", "teamLead", "members", "parentTeam", "subTeams" })
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long teamId;

    private String teamName;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    @JsonBackReference(value = "department-teams")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_lead_id", nullable = false)
    @JsonBackReference(value = "user-led-teams")
    private User teamLead;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "team-members")
    private List<TeamMember> members = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_team_id")
    @JsonBackReference(value = "team-subteams")
    private Team parentTeam;

    @OneToMany(mappedBy = "parentTeam", fetch = FetchType.LAZY)
    @JsonManagedReference(value = "team-subteams")
    private List<Team> subTeams = new ArrayList<>();

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public void addMember(TeamMember member) {
        if (!this.members.contains(member)) {
            this.members.add(member);
            member.setTeam(this);
        }
    }

    public void removeMember(TeamMember member) {
        if (this.members.remove(member)) {
            member.setTeam(null);
        }
    }
}
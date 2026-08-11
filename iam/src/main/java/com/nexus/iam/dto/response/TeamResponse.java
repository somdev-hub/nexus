package com.nexus.iam.dto.response;

import com.nexus.iam.entities.enums.TeamRole;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class TeamResponse {

    private Long teamId;
    private String teamName;
    private String description;
    private Long departmentId;
    private String departmentName;
    private TeamMemberSummary teamLead;
    private Long parentTeamId;
    private String parentTeamName;
    private List<TeamSummary> subTeams;
    private Integer memberCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    @Data
    public static class TeamMemberSummary {
        private Long id;
        private Long userId;
        private String userName;
        private String userEmail;
        private TeamRole role;
        private Integer hierarchyLevel;
        private String teamPosition;
    }

    @Data
    public static class TeamSummary {
        private Long teamId;
        private String teamName;
        private Integer memberCount;
    }
}
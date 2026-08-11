package com.nexus.iam.dto.response;

import com.nexus.iam.entities.enums.TeamRole;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class TeamMemberResponse {

    private Long id;
    private Long teamId;
    private UserSummary user;
    private TeamMemberSummary manager;
    private List<TeamMemberSummary> subordinates;
    private TeamRole role;
    private Integer hierarchyLevel;
    private String teamPosition;
    private Timestamp assignedAt;
    private Boolean isActive;

    @Data
    public static class UserSummary {
        private Long id;
        private String name;
        private String email;
        private String profilePhoto;
    }

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
}
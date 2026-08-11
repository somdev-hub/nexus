package com.nexus.iam.dto.response;

import com.nexus.iam.entities.enums.TeamRole;
import lombok.Data;

import java.util.List;

@Data
public class TeamHierarchyResponse {

    private Long teamId;
    private String teamName;
    private HierarchyNode root;

    @Data
    public static class HierarchyNode {
        private TeamMemberSummary member;
        private List<HierarchyNode> children;

        @Data
        public static class TeamMemberSummary {
            private Long id;
            private Long userId;
            private String userName;
            private String userEmail;
            private String profilePhoto;
            private TeamRole role;
            private Integer hierarchyLevel;
            private String teamPosition;
        }
    }
}
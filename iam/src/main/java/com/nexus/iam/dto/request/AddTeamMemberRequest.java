package com.nexus.iam.dto.request;

import com.nexus.iam.entities.enums.TeamRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddTeamMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private Long managerId;

    @NotBlank(message = "Team position is required")
    private String teamPosition;

    @NotNull(message = "Role is required")
    private TeamRole role;
}
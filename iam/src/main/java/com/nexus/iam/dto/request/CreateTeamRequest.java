package com.nexus.iam.dto.request;

import com.nexus.iam.entities.enums.TeamRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTeamRequest {

    @NotBlank(message = "Team name is required")
    private String teamName;

    private String description;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotNull(message = "Team lead user ID is required")
    private Long teamLeadId;

    private Long parentTeamId;
}
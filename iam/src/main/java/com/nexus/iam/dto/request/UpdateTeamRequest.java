package com.nexus.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTeamRequest {

    @NotBlank(message = "Team name is required")
    private String teamName;

    private String description;

    private Long teamLeadId;

    private Long parentTeamId;
}
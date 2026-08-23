package com.nexus.iam.dto.request;

import com.nexus.iam.entities.enums.TeamRole;
import lombok.Data;

@Data
public class UpdateTeamMemberRequest {
	private String teamPosition;
	private TeamRole role;
	private Long managerId;
}
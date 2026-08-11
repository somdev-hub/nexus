package com.nexus.iam.service;

import com.nexus.iam.dto.request.AddTeamMemberRequest;
import com.nexus.iam.dto.request.ChangeManagerRequest;
import com.nexus.iam.dto.request.CreateTeamRequest;
import com.nexus.iam.dto.request.UpdateTeamMemberRequest;
import com.nexus.iam.dto.request.UpdateTeamRequest;
import com.nexus.iam.dto.response.TeamHierarchyResponse;
import com.nexus.iam.dto.response.TeamMemberResponse;
import com.nexus.iam.dto.response.TeamResponse;
import com.nexus.iam.entities.User;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface TeamService {
	ResponseEntity<?> createTeam(CreateTeamRequest request, String authHeader);

	ResponseEntity<?> getTeam(Long teamId);

	ResponseEntity<?> getTeamsByDepartment(Long departmentId);

	ResponseEntity<?> getAllTeamsByDepartment(Long departmentId);

	ResponseEntity<?> getEligibleLeads(Long departmentId);

	ResponseEntity<?> updateTeam(Long teamId, UpdateTeamRequest request);

	ResponseEntity<Void> deleteTeam(Long teamId);

	ResponseEntity<?> addMember(Long teamId, AddTeamMemberRequest request);

	ResponseEntity<?> getMembers(Long teamId);

	ResponseEntity<?> getHierarchy(Long teamId);

	ResponseEntity<?> updateMember(Long memberId, UpdateTeamMemberRequest request);

	ResponseEntity<?> changeManager(Long memberId, ChangeManagerRequest request);

	ResponseEntity<Void> removeMember(Long memberId);

	ResponseEntity<?> getSubordinates(Long memberId);

	ResponseEntity<?> getUserManagedTeams(Long userId);

	ResponseEntity<?> getTeamLead(Long teamId);

	ResponseEntity<?> getManagers(Long teamId);
}
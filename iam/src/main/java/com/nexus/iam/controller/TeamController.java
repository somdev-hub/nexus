package com.nexus.iam.controller;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.dto.request.*;
import com.nexus.iam.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/iam/team")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class TeamController {
	private final TeamService teamService;

	@LogActivity("Create Team")
	@PostMapping("/create")
	public ResponseEntity<?> createTeam(@RequestBody CreateTeamRequest request,
			@RequestHeader("Authorization") String authHeader) {
		return teamService.createTeam(request, authHeader);
	}

	@LogActivity("Get Team")
	@GetMapping("/{teamId}")
	public ResponseEntity<?> getTeam(@PathVariable Long teamId) {
		return teamService.getTeam(teamId);
	}

	@LogActivity("Get Teams by Department")
	@GetMapping("/department/{deptId}")
	public ResponseEntity<?> getTeamsByDepartment(@PathVariable Long deptId) {
		return teamService.getTeamsByDepartment(deptId);
	}

	@LogActivity("Get All Teams by Department")
	@GetMapping("/department/{deptId}/all")
	public ResponseEntity<?> getAllTeamsByDepartment(@PathVariable Long deptId,
			@RequestHeader("Authorization") String authHeader) {
		return teamService.getAllTeamsByDepartment(deptId);
	}

	@LogActivity("Get Eligible Team Leads")
	@GetMapping("/department/{deptId}/eligible-leads")
	public ResponseEntity<?> getEligibleLeads(@PathVariable Long deptId) {
		return teamService.getEligibleLeads(deptId);
	}

	@LogActivity("Update Team")
	@PutMapping("/{teamId}")
	public ResponseEntity<?> updateTeam(@PathVariable Long teamId, @RequestBody UpdateTeamRequest request) {
		return teamService.updateTeam(teamId, request);
	}

	@LogActivity("Delete Team")
	@DeleteMapping("/{teamId}")
	public ResponseEntity<Void> deleteTeam(@PathVariable Long teamId) {
		teamService.deleteTeam(teamId);
		return ResponseEntity.ok().build();
	}

	@LogActivity("Add Team Member")
	@PostMapping("/{teamId}/member/add")
	public ResponseEntity<?> addMember(@PathVariable Long teamId, @RequestBody AddTeamMemberRequest request) {
		return teamService.addMember(teamId, request);
	}

	@LogActivity("Get Team Members")
	@GetMapping("/{teamId}/members")
	public ResponseEntity<?> getMembers(@PathVariable Long teamId) {
		return teamService.getMembers(teamId);
	}

	@LogActivity("Get Team Hierarchy")
	@GetMapping("/{teamId}/hierarchy")
	public ResponseEntity<?> getHierarchy(@PathVariable Long teamId,
			@RequestHeader("Authorization") String authHeader) {
		return teamService.getHierarchy(teamId);
	}

	@LogActivity("Update Team Member")
	@PutMapping("/member/{memberId}")
	public ResponseEntity<?> updateMember(@PathVariable Long memberId, @RequestBody UpdateTeamMemberRequest request) {
		return teamService.updateMember(memberId, request);
	}

	@LogActivity("Change Member Manager")
	@PutMapping("/member/{memberId}/manager")
	public ResponseEntity<?> changeManager(@PathVariable Long memberId, @RequestBody ChangeManagerRequest request) {
		return teamService.changeManager(memberId, request);
	}

	@LogActivity("Remove Team Member")
	@DeleteMapping("/member/{memberId}")
	public ResponseEntity<Void> removeMember(@PathVariable Long memberId) {
		teamService.removeMember(memberId);
		return ResponseEntity.ok().build();
	}

	@LogActivity("Get Member Subordinates")
	@GetMapping("/member/{memberId}/subordinates")
	public ResponseEntity<?> getSubordinates(@PathVariable Long memberId) {
		return teamService.getSubordinates(memberId);
	}

	@LogActivity("Get User Managed Teams")
	@GetMapping("/user/{userId}/managed-teams")
	public ResponseEntity<?> getUserManagedTeams(@PathVariable Long userId) {
		return teamService.getUserManagedTeams(userId);
	}

	@LogActivity("Get Team Lead")
	@GetMapping("/{teamId}/lead")
	public ResponseEntity<?> getTeamLead(@PathVariable Long teamId) {
		return teamService.getTeamLead(teamId);
	}

	@LogActivity("Get Team Managers")
	@GetMapping("/{teamId}/managers")
	public ResponseEntity<?> getManagers(@PathVariable Long teamId) {
		return teamService.getManagers(teamId);
	}
}
package com.nexus.iam.service.impl;

import com.nexus.iam.dto.request.*;
import com.nexus.iam.dto.response.TeamHierarchyResponse;
import com.nexus.iam.dto.response.TeamMemberResponse;
import com.nexus.iam.dto.response.TeamResponse;
import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.Team;
import com.nexus.iam.entities.TeamMember;
import com.nexus.iam.entities.User;
import com.nexus.iam.entities.enums.TeamRole;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.repository.DepartmentRepository;
import com.nexus.iam.repository.TeamMemberRepository;
import com.nexus.iam.repository.TeamRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.security.JwtUtil;
import com.nexus.iam.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamServiceImpl implements TeamService {
	private static final int MAX_HIERARCHY_DEPTH = 10;
	private final TeamRepository teamRepository;
	private final TeamMemberRepository teamMemberRepository;
	private final DepartmentRepository departmentRepository;
	private final UserRepository userRepository;
	private final JwtUtil jwtUtil;

	@Override
	@Transactional
	public ResponseEntity<?> createTeam(CreateTeamRequest request, String authHeader) {
		validateToken(authHeader);
		Department department = departmentRepository.findById(request.getDepartmentId())
				.orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
		if (teamRepository.existsByTeamNameAndDepartmentAndIsActiveTrue(request.getTeamName(), department)) {
			throw new IllegalArgumentException(
					"Team with name '" + request.getTeamName() + "' already exists in this department");
		}
		User teamLead = userRepository.findById(request.getTeamLeadId())
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getTeamLeadId()));
		if (!department.getMembers().contains(teamLead)) {
			throw new IllegalArgumentException("Team lead must be a member of the department");
		}
		Team parentTeam = null;
		if (request.getParentTeamId() != null) {
			parentTeam = teamRepository.findByTeamIdAndIsActiveTrue(request.getParentTeamId())
					.orElseThrow(() -> new ResourceNotFoundException("Parent Team", "id", request.getParentTeamId()));
			if (!parentTeam.getDepartment().equals(department)) {
				throw new IllegalArgumentException("Parent team must belong to the same department");
			}
		}
		Team team = new Team();
		team.setTeamName(request.getTeamName());
		team.setDescription(request.getDescription());
		team.setDepartment(department);
		team.setTeamLead(teamLead);
		team.setParentTeam(parentTeam);
		Team savedTeam = teamRepository.save(team);

		TeamMember leadMember = new TeamMember();
		leadMember.setTeam(savedTeam);
		leadMember.setUser(teamLead);
		leadMember.setRole(TeamRole.TEAM_LEAD);
		leadMember.setHierarchyLevel(0);
		leadMember.setTeamPosition("Team Lead");
		leadMember.setIsActive(true);
		teamMemberRepository.save(leadMember);
		savedTeam.addMember(leadMember);

		log.info("Created team '{}' with lead '{}'", savedTeam.getTeamName(), teamLead.getName());
		return ResponseEntity.ok(mapToTeamResponse(savedTeam));
	}

	@Override
	public ResponseEntity<?> getTeam(Long teamId) {
		Team team = teamRepository.findByTeamIdAndIsActiveTrue(teamId)
				.orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
		return ResponseEntity.ok(mapToTeamResponse(team));
	}

	@Override
	public ResponseEntity<?> getTeamsByDepartment(Long departmentId) {
		Department department = departmentRepository.findById(departmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));
		List<Team> teams = teamRepository.findByDepartmentAndParentTeamIsNullAndIsActiveTrue(department);
		return ResponseEntity.ok(teams.stream().map(this::mapToTeamResponse).collect(Collectors.toList()));
	}

	@Override
	public ResponseEntity<?> getAllTeamsByDepartment(Long departmentId) {
		Department department = departmentRepository.findById(departmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));
		List<Team> teams = teamRepository.findByDepartmentAndIsActiveTrue(department);
		return ResponseEntity.ok(teams.stream().map(this::mapToTeamResponse).collect(Collectors.toList()));
	}

	@Override
	public ResponseEntity<?> getEligibleLeads(Long departmentId) {
		departmentRepository.findById(departmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));
		return ResponseEntity.ok(userRepository.findAllByDepartmentId(departmentId));
	}

	@Override
	@Transactional
	public ResponseEntity<?> updateTeam(Long teamId, UpdateTeamRequest request) {
		Team team = teamRepository.findByTeamIdAndIsActiveTrue(teamId)
				.orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
		if (request.getTeamName() != null && !request.getTeamName().equals(team.getTeamName())) {
			if (teamRepository.existsByTeamNameAndDepartmentAndIsActiveTrue(request.getTeamName(),
					team.getDepartment())) {
				throw new IllegalArgumentException(
						"Team with name '" + request.getTeamName() + "' already exists in this department");
			}
			team.setTeamName(request.getTeamName());
		}
		if (request.getDescription() != null) {
			team.setDescription(request.getDescription());
		}
		if (request.getTeamLeadId() != null) {
			User newLead = userRepository.findById(request.getTeamLeadId())
					.orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getTeamLeadId()));
			if (!team.getDepartment().getMembers().contains(newLead)) {
				throw new IllegalArgumentException("New team lead must be a member of the department");
			}
			team.setTeamLead(newLead);
			teamMemberRepository.findByTeamAndRole(team, TeamRole.TEAM_LEAD).forEach(tm -> tm.setIsActive(false));
			TeamMember newLeadMember = teamMemberRepository.findByTeamAndUserAndIsActive(team, newLead, true)
					.orElseGet(() -> {
						TeamMember tm = new TeamMember();
						tm.setTeam(team);
						tm.setUser(newLead);
						tm.setRole(TeamRole.TEAM_LEAD);
						tm.setHierarchyLevel(0);
						tm.setTeamPosition("Team Lead");
						tm.setIsActive(true);
						return teamMemberRepository.save(tm);
					});
			newLeadMember.setRole(TeamRole.TEAM_LEAD);
			newLeadMember.setHierarchyLevel(0);
			newLeadMember.setTeamPosition("Team Lead");
			teamMemberRepository.save(newLeadMember);
		}
		if (request.getParentTeamId() != null) {
			Team parentTeam = teamRepository.findByTeamIdAndIsActiveTrue(request.getParentTeamId())
					.orElseThrow(() -> new ResourceNotFoundException("Parent Team", "id", request.getParentTeamId()));
			if (!parentTeam.getDepartment().equals(team.getDepartment())) {
				throw new IllegalArgumentException("Parent team must belong to the same department");
			}
			if (wouldCreateCycle(team, parentTeam)) {
				throw new IllegalArgumentException("Cannot set parent team: would create a cycle");
			}
			team.setParentTeam(parentTeam);
		}
		Team updatedTeam = teamRepository.save(team);
		return ResponseEntity.ok(mapToTeamResponse(updatedTeam));
	}

	@Override
	@Transactional
	public ResponseEntity<Void> deleteTeam(Long teamId) {
		Team team = teamRepository.findByTeamIdAndIsActiveTrue(teamId)
				.orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
		List<Team> subTeams = teamRepository.findByParentTeamAndIsActiveTrue(team);
		if (!subTeams.isEmpty()) {
			throw new IllegalArgumentException(
					"Cannot delete team with sub-teams. Delete or reassign sub-teams first.");
		}
		List<TeamMember> members = teamMemberRepository.findByTeamAndIsActive(team, true);
		members.forEach(m -> m.setIsActive(false));
		teamMemberRepository.saveAll(members);
		team.setIsActive(false);
		teamRepository.save(team);
		log.info("Soft deleted team '{}'", team.getTeamName());
		return ResponseEntity.ok().build();
	}

	@Override
	@Transactional
	public ResponseEntity<?> addMember(Long teamId, AddTeamMemberRequest request) {
		Team team = teamRepository.findByTeamIdAndIsActiveTrue(teamId)
				.orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
		User user = userRepository.findById(request.getUserId())
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
		if (!team.getDepartment().getMembers().contains(user)) {
			throw new IllegalArgumentException("User must be a member of the team's department");
		}
		if (teamMemberRepository.existsByTeamAndUserAndIsActive(team, user, true)) {
			throw new IllegalArgumentException("User is already an active member of this team");
		}
		TeamMember manager;
		int hierarchyLevel;
		if (request.getManagerId() != null) {
			manager = teamMemberRepository.findById(request.getManagerId())
					.orElseThrow(() -> new ResourceNotFoundException("Manager", "id", request.getManagerId()));
			if (!manager.getTeam().equals(team)) {
				throw new IllegalArgumentException("Manager must belong to the same team");
			}
			if (!manager.getIsActive()) {
				throw new IllegalArgumentException("Manager must be an active team member");
			}
			if (manager.getRole() == TeamRole.EMPLOYEE) {
				throw new IllegalArgumentException("An employee cannot be a manager");
			}
			hierarchyLevel = manager.getHierarchyLevel() + 1;
			if (hierarchyLevel > MAX_HIERARCHY_DEPTH) {
				throw new IllegalArgumentException("Maximum hierarchy depth (" + MAX_HIERARCHY_DEPTH + ") exceeded");
			}
			if (wouldCreateCycle(manager,
					teamMemberRepository.findByTeamAndUserAndIsActive(team, user, false).orElse(null))) {
				throw new IllegalArgumentException("Cannot assign manager: would create a cycle");
			}
		} else {
			manager = teamMemberRepository.findTeamLead(teamId)
					.orElseThrow(() -> new IllegalStateException("Team lead not found"));
			hierarchyLevel = 1;
		}
		if (request.getRole() == TeamRole.TEAM_LEAD && hierarchyLevel != 0) {
			throw new IllegalArgumentException("Team lead must be at hierarchy level 0");
		}
		if (request.getRole() == TeamRole.MANAGER && hierarchyLevel == 0) {
			throw new IllegalArgumentException("Manager cannot be at hierarchy level 0");
		}
		if (request.getRole() == TeamRole.EMPLOYEE && hierarchyLevel == 0) {
			throw new IllegalArgumentException("Employee cannot be at hierarchy level 0");
		}
		TeamMember member = new TeamMember();
		member.setTeam(team);
		member.setUser(user);
		member.setManager(manager);
		member.setRole(request.getRole());
		member.setHierarchyLevel(hierarchyLevel);
		member.setTeamPosition(request.getTeamPosition());
		member.setIsActive(true);
		TeamMember savedMember = teamMemberRepository.save(member);
		manager.addSubordinate(savedMember);
		teamMemberRepository.save(manager);
		team.addMember(savedMember);
		log.info("Added user '{}' to team '{}' as {}", user.getName(), team.getTeamName(), request.getRole());
		return ResponseEntity.ok(mapToTeamMemberResponse(savedMember));
	}

	@Override
	public ResponseEntity<?> getMembers(Long teamId) {
		Team team = teamRepository.findByTeamIdAndIsActiveTrue(teamId)
				.orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
		List<TeamMember> members = teamMemberRepository.findByTeamAndIsActive(team, true);
		return ResponseEntity.ok(members.stream().map(this::mapToTeamMemberResponse).collect(Collectors.toList()));
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<?> getHierarchy(Long teamId) {
		Team team = teamRepository.findByTeamIdAndIsActiveTrue(teamId)
				.orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
		List<TeamMember> allMembers = teamMemberRepository.findByTeamWithManager(teamId);
		TeamMember teamLead = allMembers.stream()
				.filter(m -> m.getRole() == TeamRole.TEAM_LEAD && m.getHierarchyLevel() == 0)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Team lead not found for team: " + teamId));
		Map<Long, List<TeamMember>> childrenMap = new HashMap<>();
		allMembers.forEach(member -> {
			if (member.getManager() != null) {
				Long managerId = member.getManager().getId();
				childrenMap.computeIfAbsent(managerId, k -> new ArrayList<>()).add(member);
			}
		});
		TeamHierarchyResponse response = new TeamHierarchyResponse();
		response.setTeamId(team.getTeamId());
		response.setTeamName(team.getTeamName());
		response.setRoot(buildHierarchyNodeFromMap(teamLead, childrenMap));
		return ResponseEntity.ok(response);
	}

	@Override
	@Transactional
	public ResponseEntity<?> updateMember(Long memberId, UpdateTeamMemberRequest request) {
		TeamMember member = teamMemberRepository.findById(memberId)
				.orElseThrow(() -> new ResourceNotFoundException("TeamMember", "id", memberId));
		if (request.getTeamPosition() != null) {
			member.setTeamPosition(request.getTeamPosition());
		}
		if (request.getRole() != null && !request.getRole().equals(member.getRole())) {
			if (request.getRole() == TeamRole.TEAM_LEAD && member.getHierarchyLevel() != 0) {
				throw new IllegalArgumentException("Only hierarchy level 0 can be team lead");
			}
			if (request.getRole() == TeamRole.MANAGER && member.getHierarchyLevel() == 0) {
				throw new IllegalArgumentException("Manager cannot be at hierarchy level 0");
			}
			if (request.getRole() == TeamRole.EMPLOYEE && member.getHierarchyLevel() == 0) {
				throw new IllegalArgumentException("Employee cannot be at hierarchy level 0");
			}
			if (member.getRole() == TeamRole.MANAGER && request.getRole() == TeamRole.EMPLOYEE) {
				TeamMember newManager = member.getManager();
				if (newManager == null) {
					newManager = teamMemberRepository.findTeamLead(member.getTeam().getTeamId())
							.orElseThrow(() -> new IllegalStateException(
									"Team lead not found for team: " + member.getTeam().getTeamId()));
				}
				reassignSubordinatesToManager(member, newManager);
			}
			member.setRole(request.getRole());
		}
		TeamMember updated = teamMemberRepository.save(member);
		return ResponseEntity.ok(mapToTeamMemberResponse(updated));
	}

	@Override
	@Transactional
	public ResponseEntity<?> changeManager(Long memberId, ChangeManagerRequest request) {
		TeamMember member = teamMemberRepository.findById(memberId)
				.orElseThrow(() -> new ResourceNotFoundException("TeamMember", "id", memberId));
		TeamMember newManager;
		int newHierarchyLevel;
		if (request.getNewManagerId() != null) {
			newManager = teamMemberRepository.findById(request.getNewManagerId())
					.orElseThrow(() -> new ResourceNotFoundException("Manager", "id", request.getNewManagerId()));
			if (!newManager.getTeam().equals(member.getTeam())) {
				throw new IllegalArgumentException("New manager must belong to the same team");
			}
			if (!newManager.getIsActive()) {
				throw new IllegalArgumentException("New manager must be an active team member");
			}
			if (newManager.getRole() == TeamRole.EMPLOYEE) {
				throw new IllegalArgumentException("An employee cannot be a manager");
			}
			if (newManager.getRole() != TeamRole.MANAGER && newManager.getRole() != TeamRole.TEAM_LEAD) {
				throw new IllegalArgumentException("New manager must have a manager or team lead role");
			}
			if (wouldCreateCycle(newManager, member)) {
				throw new IllegalArgumentException("Cannot assign manager: would create a cycle");
			}
			newHierarchyLevel = newManager.getHierarchyLevel() + 1;
			if (newHierarchyLevel > MAX_HIERARCHY_DEPTH) {
				throw new IllegalArgumentException("Maximum hierarchy depth (" + MAX_HIERARCHY_DEPTH + ") exceeded");
			}
		} else {
			newManager = teamMemberRepository.findTeamLead(member.getTeam().getTeamId())
					.orElseThrow(() -> new IllegalStateException("Team lead not found"));
			newHierarchyLevel = 1;
		}
		if (member.getManager() != null) {
			member.getManager().removeSubordinate(member);
			teamMemberRepository.save(member.getManager());
		}
		member.setManager(newManager);
		member.setHierarchyLevel(newHierarchyLevel);
		newManager.addSubordinate(member);
		teamMemberRepository.save(newManager);
		updateSubordinateHierarchyLevels(member, newHierarchyLevel);
		TeamMember updated = teamMemberRepository.save(member);
		return ResponseEntity.ok(mapToTeamMemberResponse(updated));
	}

	@Override
	@Transactional
	public ResponseEntity<Void> removeMember(Long memberId) {
		TeamMember member = teamMemberRepository.findById(memberId)
				.orElseThrow(() -> new ResourceNotFoundException("TeamMember", "id", memberId));
		if (member.isTeamLead()) {
			List<TeamMember> directSubordinates = teamMemberRepository.findByManagerAndIsActive(member, true);
			if (directSubordinates.isEmpty()) {
				throw new IllegalStateException("Cannot remove team lead with no subordinates");
			}
			TeamMember newLead = directSubordinates.stream()
					.min(Comparator.comparing(m -> m.getAssignedAt()))
					.orElseThrow(() -> new IllegalStateException("No valid team lead candidate found"));
			newLead.setRole(TeamRole.TEAM_LEAD);
			newLead.setHierarchyLevel(0);
			teamMemberRepository.save(newLead);
			Team team = member.getTeam();
			team.setTeamLead(newLead.getUser());
			teamRepository.save(team);
			for (TeamMember sub : directSubordinates) {
				if (!sub.getId().equals(newLead.getId())) {
					sub.setManager(newLead);
					sub.setHierarchyLevel(1);
					teamMemberRepository.save(sub);
				}
			}
			team.removeMember(member);
			teamMemberRepository.delete(member);
			log.info("Removed team lead '{}' and promoted '{}' as new team lead", member.getUser().getName(),
					newLead.getUser().getName());
			return ResponseEntity.ok().build();
		}
		TeamMember manager = member.getManager();
		List<TeamMember> activeMembers = teamMemberRepository.findActiveMembersByTeamId(member.getTeam().getTeamId());
		if (!member.getSubordinates().isEmpty()) {
			TeamMember newManager = manager;
			if (newManager == null) {
				newManager = activeMembers.stream()
						.filter(m -> m.isTeamLead())
						.findFirst()
						.orElseThrow(() -> new IllegalStateException(
								"Team lead not found for team: " + member.getTeam().getTeamId()));
			}
			reassignSubordinatesToManager(member, newManager);
		}
		if (manager != null) {
			manager.removeSubordinate(member);
			teamMemberRepository.save(manager);
		}
		member.getTeam().removeMember(member);
		teamMemberRepository.delete(member);
		log.info("Removed member '{}' from team '{}'", member.getUser().getName(), member.getTeam().getTeamName());
		return ResponseEntity.ok().build();
	}

	@Override
	public ResponseEntity<?> getSubordinates(Long memberId) {
		TeamMember member = teamMemberRepository.findById(memberId)
				.orElseThrow(() -> new ResourceNotFoundException("TeamMember", "id", memberId));
		List<TeamMember> subordinates = teamMemberRepository.findByManagerAndIsActive(member, true);
		return ResponseEntity.ok(subordinates.stream().map(this::mapToTeamMemberResponse).collect(Collectors.toList()));
	}

	@Override
	public ResponseEntity<?> getUserManagedTeams(Long userId) {
		userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
		List<Team> ledTeams = teamRepository.findTeamsWhereUserIsLead(userId);
		List<TeamMember> managedMemberships = teamMemberRepository.findActiveMembershipsByUserId(userId)
				.stream()
				.filter(tm -> tm.getRole() == TeamRole.TEAM_LEAD || tm.getRole() == TeamRole.MANAGER)
				.toList();
		Map<Long, Team> teamMap = new HashMap<>();
		ledTeams.forEach(t -> teamMap.put(t.getTeamId(), t));
		managedMemberships.forEach(tm -> teamMap.put(tm.getTeam().getTeamId(), tm.getTeam()));
		return ResponseEntity.ok(teamMap.values().stream().map(this::mapToTeamResponse).collect(Collectors.toList()));
	}

	@Override
	public ResponseEntity<?> getTeamLead(Long teamId) {
		teamRepository.findByTeamIdAndIsActiveTrue(teamId)
				.orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
		TeamMember lead = teamMemberRepository.findTeamLead(teamId)
				.orElseThrow(() -> new ResourceNotFoundException("TeamLead", "teamId", teamId));
		return ResponseEntity.ok(mapToTeamMemberResponse(lead));
	}

	@Override
	public ResponseEntity<?> getManagers(Long teamId) {
		teamRepository.findByTeamIdAndIsActiveTrue(teamId)
				.orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
		List<TeamMember> managers = teamMemberRepository.findManagers(teamId);
		return ResponseEntity.ok(managers.stream().filter(m -> m.getIsActive()).map(this::mapToTeamMemberResponse)
				.collect(Collectors.toList()));
	}

	// ============ Helper Methods ============
	private void validateToken(String authHeader) {
		if (ObjectUtils.isEmpty(authHeader) || !jwtUtil.isValidToken(authHeader)) {
			throw new ServiceLevelException("TeamService", "Invalid or missing token", "validateToken", "Unauthorized",
					"Invalid token");
		}
	}

	private boolean wouldCreateCycle(TeamMember potentialManager, TeamMember potentialSubordinate) {
		if (potentialSubordinate == null)
			return false;
		TeamMember current = potentialManager;
		while (current != null) {
			if (current.getId().equals(potentialSubordinate.getId())) {
				return true;
			}
			current = current.getManager();
		}
		return false;
	}

	private boolean wouldCreateCycle(Team team, Team potentialParent) {
		Team current = potentialParent;
		while (current != null) {
			if (current.getTeamId().equals(team.getTeamId())) {
				return true;
			}
			current = current.getParentTeam();
		}
		return false;
	}

	private void reassignSubordinatesToManager(TeamMember oldManager, TeamMember newManager) {
		List<TeamMember> subordinates = new ArrayList<>(oldManager.getSubordinates());
		for (TeamMember sub : subordinates) {
			oldManager.removeSubordinate(sub);
			sub.setManager(newManager);
			if (newManager != null) {
				sub.setHierarchyLevel(newManager.getHierarchyLevel() + 1);
				newManager.addSubordinate(sub);
			} else {
				sub.setHierarchyLevel(1);
			}
			teamMemberRepository.save(sub);
			updateSubordinateHierarchyLevels(sub, sub.getHierarchyLevel());
		}
		if (newManager != null) {
			teamMemberRepository.save(newManager);
		}
		teamMemberRepository.save(oldManager);
	}

	private void updateSubordinateHierarchyLevels(TeamMember manager, int managerLevel) {
		for (TeamMember sub : manager.getSubordinates()) {
			if (sub.getIsActive()) {
				sub.setHierarchyLevel(managerLevel + 1);
				teamMemberRepository.save(sub);
				updateSubordinateHierarchyLevels(sub, sub.getHierarchyLevel());
			}
		}
	}

	private TeamResponse mapToTeamResponse(Team team) {
		TeamResponse response = new TeamResponse();
		response.setTeamId(team.getTeamId());
		response.setTeamName(team.getTeamName());
		response.setDescription(team.getDescription());
		response.setDepartmentId(team.getDepartment().getDepartmentId());
		response.setDepartmentName(team.getDepartment().getDepartmentName());
		response.setParentTeamId(team.getParentTeam() != null ? team.getParentTeam().getTeamId() : null);
		response.setParentTeamName(team.getParentTeam() != null ? team.getParentTeam().getTeamName() : null);
		response.setCreatedAt(team.getCreatedAt());
		response.setUpdatedAt(team.getUpdatedAt());
		TeamMember leadMember = teamMemberRepository.findTeamLead(team.getTeamId()).orElse(null);
		if (leadMember != null) {
			TeamResponse.TeamMemberSummary leadSummary = new TeamResponse.TeamMemberSummary();
			leadSummary.setId(leadMember.getId());
			leadSummary.setUserId(leadMember.getUser().getId());
			leadSummary.setUserName(leadMember.getUser().getName());
			leadSummary.setUserEmail(leadMember.getUser().getEmail());
			leadSummary.setRole(leadMember.getRole());
			leadSummary.setHierarchyLevel(leadMember.getHierarchyLevel());
			leadSummary.setTeamPosition(leadMember.getTeamPosition());
			response.setTeamLead(leadSummary);
		}
		response.setMemberCount((int) teamMemberRepository.countByTeamAndIsActive(team, true));
		List<Team> subTeams = teamRepository.findByParentTeamAndIsActiveTrue(team);
		response.setSubTeams(subTeams.stream().map(st -> {
			TeamResponse.TeamSummary summary = new TeamResponse.TeamSummary();
			summary.setTeamId(st.getTeamId());
			summary.setTeamName(st.getTeamName());
			summary.setMemberCount((int) teamMemberRepository.countByTeamAndIsActive(st, true));
			return summary;
		}).collect(Collectors.toList()));
		return response;
	}

	private TeamMemberResponse mapToTeamMemberResponse(TeamMember member) {
		TeamMemberResponse response = new TeamMemberResponse();
		response.setId(member.getId());
		response.setTeamId(member.getTeam().getTeamId());
		response.setRole(member.getRole());
		response.setHierarchyLevel(member.getHierarchyLevel());
		response.setTeamPosition(member.getTeamPosition());
		response.setAssignedAt(member.getAssignedAt());
		response.setIsActive(member.getIsActive());
		TeamMemberResponse.UserSummary userSummary = new TeamMemberResponse.UserSummary();
		userSummary.setId(member.getUser().getId());
		userSummary.setName(member.getUser().getName());
		userSummary.setEmail(member.getUser().getEmail());
		userSummary.setProfilePhoto(member.getUser().getProfilePhoto());
		response.setUser(userSummary);
		if (member.getManager() != null) {
			TeamMemberResponse.TeamMemberSummary managerSummary = new TeamMemberResponse.TeamMemberSummary();
			managerSummary.setId(member.getManager().getId());
			managerSummary.setUserId(member.getManager().getUser().getId());
			managerSummary.setUserName(member.getManager().getUser().getName());
			managerSummary.setUserEmail(member.getManager().getUser().getEmail());
			managerSummary.setRole(member.getManager().getRole());
			managerSummary.setHierarchyLevel(member.getManager().getHierarchyLevel());
			managerSummary.setTeamPosition(member.getManager().getTeamPosition());
			response.setManager(managerSummary);
		}
		List<TeamMemberResponse.TeamMemberSummary> subordinates = member.getSubordinates().stream()
				.filter(m -> m.getIsActive())
				.map(sub -> {
					TeamMemberResponse.TeamMemberSummary subSummary = new TeamMemberResponse.TeamMemberSummary();
					subSummary.setId(sub.getId());
					subSummary.setUserId(sub.getUser().getId());
					subSummary.setUserName(sub.getUser().getName());
					subSummary.setUserEmail(sub.getUser().getEmail());
					subSummary.setRole(sub.getRole());
					subSummary.setHierarchyLevel(sub.getHierarchyLevel());
					subSummary.setTeamPosition(sub.getTeamPosition());
					return subSummary;
				})
				.collect(Collectors.toList());
		response.setSubordinates(subordinates);
		return response;
	}

	private TeamHierarchyResponse.HierarchyNode buildHierarchyNodeFromMap(TeamMember member,
			Map<Long, List<TeamMember>> childrenMap) {
		TeamHierarchyResponse.HierarchyNode node = new TeamHierarchyResponse.HierarchyNode();
		TeamHierarchyResponse.HierarchyNode.TeamMemberSummary summary = new TeamHierarchyResponse.HierarchyNode.TeamMemberSummary();
		summary.setId(member.getId());
		summary.setUserId(member.getUser().getId());
		summary.setUserName(member.getUser().getName());
		summary.setUserEmail(member.getUser().getEmail());
		summary.setProfilePhoto(member.getUser().getProfilePhoto());
		summary.setRole(member.getRole());
		summary.setHierarchyLevel(member.getHierarchyLevel());
		summary.setTeamPosition(member.getTeamPosition());
		node.setMember(summary);
		List<TeamMember> children = childrenMap.getOrDefault(member.getId(), Collections.emptyList());
		List<TeamHierarchyResponse.HierarchyNode> childNodes = children.stream()
				.filter(m -> m.getIsActive())
				.map(child -> buildHierarchyNodeFromMap(child, childrenMap))
				.collect(Collectors.toList());
		node.setChildren(childNodes);
		return node;
	}
}
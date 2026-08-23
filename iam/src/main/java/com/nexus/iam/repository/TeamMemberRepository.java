package com.nexus.iam.repository;

import com.nexus.iam.entities.Team;
import com.nexus.iam.entities.TeamMember;
import com.nexus.iam.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

	List<TeamMember> findByTeam(Team team);

	List<TeamMember> findByTeamAndIsActive(Team team, boolean isActive);

	List<TeamMember> findByManager(TeamMember manager);

	List<TeamMember> findByManagerAndIsActive(TeamMember manager, boolean isActive);

	List<TeamMember> findByTeamAndUser(Team team, User user);

	Optional<TeamMember> findByTeamAndUserAndIsActive(Team team, User user, boolean isActive);

	List<TeamMember> findByTeamAndHierarchyLevel(Team team, int level);

	List<TeamMember> findByTeamAndRole(Team team, com.nexus.iam.entities.enums.TeamRole role);

	@Query("SELECT tm FROM TeamMember tm WHERE tm.user.id = :userId AND tm.isActive = true")
	List<TeamMember> findActiveMembershipsByUserId(@Param("userId") Long userId);

	@Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.manager IS NULL AND tm.hierarchyLevel > 0")
	List<TeamMember> findOrphanMembers(@Param("teamId") Long teamId);

	@Query("SELECT tm FROM TeamMember tm LEFT JOIN FETCH tm.subordinates WHERE tm.team.id = :teamId AND tm.role = 'TEAM_LEAD' AND tm.isActive = true")
	Optional<TeamMember> findTeamLead(@Param("teamId") Long teamId);

	@Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.role = 'MANAGER' AND tm.isActive = true")
	List<TeamMember> findManagers(@Param("teamId") Long teamId);

	@Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.isActive = true ORDER BY tm.hierarchyLevel ASC")
	List<TeamMember> findByTeamOrderByHierarchy(@Param("teamId") Long teamId);

	@Query("SELECT tm FROM TeamMember tm LEFT JOIN FETCH tm.manager WHERE tm.team.id = :teamId AND tm.isActive = true ORDER BY tm.hierarchyLevel ASC")
	List<TeamMember> findByTeamWithManager(@Param("teamId") Long teamId);

	Page<TeamMember> findByTeam(Team team, Pageable pageable);

	long countByTeamAndIsActive(Team team, boolean isActive);

	boolean existsByTeamAndUserAndIsActive(Team team, User user, boolean isActive);

	@Query("SELECT tm FROM TeamMember tm WHERE tm.team.teamId = :teamId AND tm.isActive = true")
	List<TeamMember> findActiveMembersByTeamId(@Param("teamId") Long teamId);
}
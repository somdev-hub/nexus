package com.nexus.iam.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.Team;
import com.nexus.iam.entities.User;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByDepartmentAndIsActiveTrue(Department department);

    List<Team> findByTeamLeadAndIsActiveTrue(User teamLead);

    Optional<Team> findByTeamNameAndDepartmentAndIsActiveTrue(String teamName, Department department);

    boolean existsByTeamNameAndDepartmentAndIsActiveTrue(String teamName, Department department);

    List<Team> findByDepartmentAndParentTeamIsNullAndIsActiveTrue(Department department);

    List<Team> findByParentTeamAndIsActiveTrue(Team parentTeam);

    Optional<Team> findByTeamIdAndIsActiveTrue(Long teamId);

    @Query("SELECT t FROM Team t WHERE t.department.organization.id = :orgId AND t.isActive = true")
    List<Team> findByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT t FROM Team t JOIN t.members tm WHERE tm.user.id = :userId AND tm.isActive = true AND t.isActive = true")
    List<Team> findTeamsByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Team t WHERE t.teamLead.id = :userId AND t.isActive = true")
    List<Team> findTeamsWhereUserIsLead(@Param("userId") Long userId);
}
package com.nexus.iam.repository;

import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long>{
    boolean existsByDepartmentNameAndOrganization(String departmentName, Organization organization);

    @Query("SELECT d FROM Department d WHERE d.organization.id = :orgId")
    List<Department> findByOrgId(Long orgId);
}

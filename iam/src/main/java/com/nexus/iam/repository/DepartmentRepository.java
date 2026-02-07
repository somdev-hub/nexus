package com.nexus.iam.repository;

import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long>{
    boolean existsByDepartmentNameAndOrganization(String departmentName, Organization organization);
}

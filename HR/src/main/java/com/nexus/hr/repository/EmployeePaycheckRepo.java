package com.nexus.hr.repository;

import com.nexus.hr.model.entities.EmployeePaycheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeePaycheckRepo extends JpaRepository<EmployeePaycheck, Long> {

    @Query("SELECT CASE WHEN COUNT(ep) > 0 THEN true ELSE false END FROM EmployeePaycheck ep WHERE ep.orgId = :orgId AND ep.deptId = :deptId AND ep.role = :role")
    boolean existsEmployeePaycheckByOrgIdAndDeptIdAndRole(Long orgId, Long deptId, String role);

    Page<EmployeePaycheck> findByOrgId(Long orgId, Pageable pageable);
}

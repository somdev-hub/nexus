package com.nexus.iam.service;

import com.nexus.iam.dto.EmployeePaycheckDto;
import org.springframework.http.ResponseEntity;

public interface DepartmentService {

    ResponseEntity<?> createDepartment(String departmentName, Long organizationId, String authHeader);

    ResponseEntity<?> getDepartmentOverview(Long orgId, String token);

    ResponseEntity<?> getAllDeptOverview(Long orgId, String token);

    ResponseEntity<?> fetchDeparmentRoles(Long deptId);

    ResponseEntity<?> fetchDepartmentRolesTable(Long orgId, Integer pageNo, Integer pageOffset);

    ResponseEntity<?> addEmployeePaycheck(EmployeePaycheckDto employeePaycheckDto, String auth);

    ResponseEntity<?> getEmployeePaychecks(Long orgId, Integer pageNo, Integer pageOffset, String authHeader);
}

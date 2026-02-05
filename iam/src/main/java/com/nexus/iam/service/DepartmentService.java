package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;

public interface DepartmentService {

    ResponseEntity<?> createDepartment(String departmentName, Long organizationId);
}

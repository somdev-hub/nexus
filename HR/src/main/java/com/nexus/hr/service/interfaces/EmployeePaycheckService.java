package com.nexus.hr.service.interfaces;

import com.nexus.hr.model.entities.EmployeePaycheck;
import org.springframework.http.ResponseEntity;

public interface EmployeePaycheckService {

    ResponseEntity<?> addEmployeePaycheck(EmployeePaycheck employeePaycheck);

    ResponseEntity<?> getEmployeePaychecks(Long orgId, Integer pageNo, Integer pageOffset);
}

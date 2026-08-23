package com.nexus.hr.service.interfaces;

import com.nexus.hr.model.entities.EmployeePaycheck;
import org.springframework.http.ResponseEntity;

public interface CompensationService {

    public ResponseEntity<?> setRolesCompensations(EmployeePaycheck employeePaycheck);
}

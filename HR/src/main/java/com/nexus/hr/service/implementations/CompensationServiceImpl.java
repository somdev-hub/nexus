package com.nexus.hr.service.implementations;

import com.nexus.hr.entity.EmployeePaycheck;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.repository.EmployeePaycheckRepo;
import com.nexus.hr.service.interfaces.CompensationService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CompensationServiceImpl implements CompensationService {

    private final EmployeePaycheckRepo employeePaycheckRepo;

    public CompensationServiceImpl(EmployeePaycheckRepo employeePaycheckRepo) {
        this.employeePaycheckRepo = employeePaycheckRepo;
    }

    @Override
    public ResponseEntity<?> setRolesCompensations(EmployeePaycheck employeePaycheck) {
        ResponseEntity<?> responseEntity = null;
        try {
            employeePaycheckRepo.save(employeePaycheck);
            responseEntity = ResponseEntity.ok("Successfully added compensation details");
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "CompensationService",
                    "Error while setting roles and compensations",
                    "setRolesCompensations",
                    e.getClass().getName(),
                    e.getMessage()
            );
        }
        return responseEntity;
    }
}

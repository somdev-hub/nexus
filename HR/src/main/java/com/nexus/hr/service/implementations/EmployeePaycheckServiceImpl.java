package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.EmployeePaycheck;
import com.nexus.hr.repository.EmployeePaycheckRepo;
import com.nexus.hr.service.interfaces.EmployeePaycheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeePaycheckServiceImpl implements EmployeePaycheckService {

    private final EmployeePaycheckRepo employeePaycheckRepo;

    @Override
    public ResponseEntity<?> addEmployeePaycheck(EmployeePaycheck employeePaycheck) {
        ResponseEntity<?> response;
        try {
            EmployeePaycheck savedPaycheck = employeePaycheckRepo.save(employeePaycheck);
            response = ResponseEntity.ok(savedPaycheck);
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "EmployeePaycheckService",
                    "An error occurred while saving the employee paycheck",
                    "addEmployeePaycheck",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
        return response;
    }
}

package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.EmployeePaycheck;
import com.nexus.hr.repository.EmployeePaycheckRepo;
import com.nexus.hr.service.interfaces.EmployeePaycheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@RequiredArgsConstructor
public class EmployeePaycheckServiceImpl implements EmployeePaycheckService {

    private final EmployeePaycheckRepo employeePaycheckRepo;

    @Override
    public ResponseEntity<?> addEmployeePaycheck(EmployeePaycheck employeePaycheck) {
        ResponseEntity<?> response;
        if (ObjectUtils.isEmpty(employeePaycheck) || ObjectUtils.isEmpty(employeePaycheck.getDeptId()) || ObjectUtils.isEmpty(employeePaycheck.getOrgId()) || ObjectUtils.isEmpty(employeePaycheck.getRole())) {
            return ResponseEntity.badRequest().body("Invalid input: Employee paycheck, department ID, organization ID and role are required");
        }
        try {
            if (employeePaycheckRepo.existsEmployeePaycheckByOrgIdAndDeptIdAndRole(employeePaycheck.getOrgId(), employeePaycheck.getDeptId(), employeePaycheck.getRole())) {
                return ResponseEntity.badRequest().body("A paycheck for the specified organization, department and role already exists");
            }
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

    @Override
    public ResponseEntity<?> getEmployeePaychecks(Long orgId, Integer pageNo, Integer pageOffset) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(pageNo) || ObjectUtils.isEmpty(pageOffset)) {
            return ResponseEntity.badRequest().body("Invalid input: Organization ID, page number and page offset are required");
        }
        ResponseEntity<?> response;
        try {
            Pageable pageable = PageRequest.of(pageNo, pageOffset);
            Page<EmployeePaycheck> employeePaychecks = employeePaycheckRepo.findByOrgId(orgId, pageable);
            response = ResponseEntity.ok(employeePaychecks);
        } catch (RuntimeException e) {
            throw new ServiceLevelException("EmployeePaycheckService", "An error occurred while fetching employee paychecks", "getEmployeePaychecks", e.getClass().getSimpleName(), e.getMessage());
        }

        return response;
    }
}

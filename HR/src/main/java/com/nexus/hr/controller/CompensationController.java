package com.nexus.hr.controller;

import com.nexus.hr.annotation.LogActivity;
import com.nexus.hr.model.entities.EmployeePaycheck;
import com.nexus.hr.exception.UnauthorizedException;
import com.nexus.hr.service.interfaces.CompensationService;
import com.nexus.hr.utils.CommonUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hr/compensations")
public class CompensationController {

    private final CompensationService compensationService;

    private final CommonUtils commonUtils;

    public CompensationController(CompensationService compensationService, CommonUtils commonUtils) {
        this.compensationService = compensationService;
        this.commonUtils = commonUtils;
    }

    @LogActivity("Create Compensation for Roles")
    @PostMapping(value = "/create/compensation", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createCompensationControllerMethod(@RequestBody EmployeePaycheck employeePaycheck, @RequestHeader(name = "Authorization") String authorization) {

        if (!ObjectUtils.isEmpty(authorization) && !commonUtils.validateToken(authorization)) {
            throw new UnauthorizedException(
                    "UNAUTHORIZED",
                    "Invalid or expired token provided."
            );
        }

        if (ObjectUtils.isEmpty(employeePaycheck)) {
            return ResponseEntity.badRequest().body("EmployeePaycheck payload is required.");
        }

        return compensationService.setRolesCompensations(employeePaycheck);
    }
}

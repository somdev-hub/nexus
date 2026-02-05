package com.nexus.iam.controller;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/iam/department")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;

    @LogActivity("Add Department")
    @PostMapping("/add")
    public ResponseEntity<?> addDepartment(@RequestParam("orgId") Long orgId, @RequestParam("deptName") String deptName) {
        return departmentService.createDepartment(deptName, orgId);
    }
}

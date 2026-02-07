package com.nexus.iam.controller;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.security.JwtUtil;
import com.nexus.iam.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/iam/department")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;
    private final JwtUtil jwtUtil;

    @LogActivity("Add Department")
    @PostMapping("/add")
    public ResponseEntity<?> addDepartment(@RequestParam("orgId") Long orgId, @RequestParam("deptName") String deptName, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (ObjectUtils.isEmpty(authHeader) || !jwtUtil.isValidToken(authHeader)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid or missing token");
        }
        return departmentService.createDepartment(deptName, orgId, authHeader);
    }
}

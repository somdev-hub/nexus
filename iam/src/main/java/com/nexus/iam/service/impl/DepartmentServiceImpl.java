package com.nexus.iam.service.impl;

import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.repository.DepartmentRepository;
import com.nexus.iam.repository.OrganizationRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.security.JwtUtil;
import com.nexus.iam.service.DepartmentService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<?> createDepartment(String departmentName, Long organizationId, String authHeader) {
        if (ObjectUtils.isEmpty(departmentName)) {
            throw new IllegalArgumentException("Department name is required");
        }
        if (ObjectUtils.isEmpty(organizationId)) {
            throw new IllegalArgumentException("Organization ID is required");
        }


        ResponseEntity<?> response;
        try {
            Claims claims = jwtUtil.extractAllClaims(authHeader.substring("Bearer ".length()));
            String subject = claims.getSubject();
            Boolean exists = userRepository.existsByEmailAndOrganizationId(subject, organizationId);
            if (!exists) {
                response = ResponseEntity.badRequest().body("User " + subject + " does not belong to the organization with ID: " + organizationId);
                return response;
            }
            Organization organization = organizationRepository.findById(organizationId).orElseThrow(() -> new ResourceNotFoundException(
                    "Organization", "id", organizationId
            ));

            // Check if department with the same name already exists in the organization
            if (departmentRepository.existsByDepartmentNameAndOrganization(departmentName, organization)) {
                throw new IllegalArgumentException(
                        "Department with name already exists in the organization: " + departmentName);
            }

            // Create and save the department
            var department = new Department();
            department.setDepartmentName(departmentName);
            department.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            department.setOrganization(organizationRepository.findById(organizationId).orElse(null));
            var savedDepartment = departmentRepository.save(department);

            response = ResponseEntity.ok(savedDepartment);

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "DepartmentServiceImpl",
                    "Failed to create department: " + e.getMessage(),
                    "createDepartment",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage()
            );
        }

        return response;

    }
}

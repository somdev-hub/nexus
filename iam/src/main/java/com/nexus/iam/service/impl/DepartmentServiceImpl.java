package com.nexus.iam.service.impl;

import com.nexus.iam.dto.response.AllDeptOverview;
import com.nexus.iam.dto.response.DeptOverview;
import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.repository.DepartmentRepository;
import com.nexus.iam.repository.OrganizationRepository;
import com.nexus.iam.repository.PermissionRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.security.JwtUtil;
import com.nexus.iam.service.DepartmentService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.sql.Timestamp;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

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

    @Override
    public ResponseEntity<?> getDepartmentOverview(Long orgId, String token) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new IllegalArgumentException("Organization ID is required");
        }
        try {
            List<Department> departments = departmentRepository.findByOrgId(orgId);
            List<DeptOverview> deptOverviews = departments.stream().map(department -> new DeptOverview(
                    department.getDepartmentName(),
                    department.getDepartmentId(),
                    department.getDepartmentHead() != null ? department.getDepartmentHead().getName() : null,
                    department.getMembers().size(),
                    department.getRoles().size()
            )).toList();
            if (deptOverviews.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(deptOverviews);
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "DepartmentServiceImpl",
                    "Failed to get department overview: " + e.getMessage(),
                    "getDepartmentOverview",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getAllDeptOverview(Long orgId, String token) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new IllegalArgumentException("Organization ID is required");
        }
        try {
            List<Department> departments = departmentRepository.findByOrgId(orgId);
            Integer totalDepartments = departments.size();
            Integer totalEmployees = departments.stream().mapToInt(department -> department.getMembers().size()).sum();
            Integer totalRoles = departments.stream().mapToInt(department -> department.getRoles().size()).sum();
            Integer totalPermissions = departments.stream().flatMap(department -> department.getRoles().stream()).flatMap(role -> permissionRepository.findByRole(role).stream()).mapToInt(permission -> 1).sum();

            return new ResponseEntity<>(new AllDeptOverview(totalDepartments, totalEmployees, totalRoles, totalPermissions), HttpStatus.OK);
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "DepartmentServiceImpl",
                    "Failed to get all department overview: " + e.getMessage(),
                    "getAllDeptOverview",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage()
            );
        }
    }
}

package com.nexus.iam.service.impl;

import com.nexus.iam.dto.response.AllDeptOverview;
import com.nexus.iam.dto.response.DeptOverview;
import com.nexus.iam.dto.response.DeptRoleTable;
import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.entities.PermissionAction;
import com.nexus.iam.entities.Role;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

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
                    department.getDepartmentId(),
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

    @Override
    public ResponseEntity<?> fetchDeparmentRoles(Long deptId) {
        if (ObjectUtils.isEmpty(deptId)) {
            throw new IllegalArgumentException("Department ID is required");
        }
        try {
            Department department = departmentRepository.findById(deptId).orElseThrow(() -> new ResourceNotFoundException(
                    "Department", "id", deptId
            ));
            return ResponseEntity.ok(department.getRoles());
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "DepartmentServiceImpl",
                    "Failed to fetch department roles: " + e.getMessage(),
                    "fetchDeparmentRoles",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> fetchDepartmentRolesTable(Long orgId, Integer pageNo, Integer pageOffset) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new IllegalArgumentException("Organization ID is required");
        }

        try {
            // Validate pagination parameters
            int pageNumber = pageNo != null && pageNo > 0 ? pageNo - 1 : 0; // Convert to 0-indexed
            int pageSize = pageOffset != null && pageOffset > 0 ? pageOffset : 10; // Default to 10 if not provided

            Pageable pageable = PageRequest.of(pageNumber, pageSize);

            // Fetch paginated departments for the organization
            Page<Department> departmentsPage = departmentRepository.findByOrgId(orgId, pageable);

            // Transform departments and their roles into DeptRoleTable records
            List<DeptRoleTable> deptRoleTables = departmentsPage.getContent()
                    .stream()
                    .flatMap(department -> department.getRoles().stream()
                            .map(role -> createDeptRoleTable(department, role)))
                    .collect(Collectors.toList());

            // Create a new Page with the transformed data
            Page<DeptRoleTable> resultPage = new PageImpl<>(
                    deptRoleTables,
                    pageable,
                    departmentsPage.getTotalElements()
            );

            if (deptRoleTables.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(resultPage);

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "DepartmentServiceImpl",
                    "Failed to fetch department roles table: " + e.getMessage(),
                    "fetchDepartmentRolesTable",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage()
            );
        }
    }

    /**
     * Helper method to create DeptRoleTable record from Department and Role
     */
    private DeptRoleTable createDeptRoleTable(Department department, Role role) {
        // Get the number of employees assigned to this role in the department
        int noOfEmployees = (int) department.getMembers().stream()
                .filter(user -> user.getRoles().contains(role))
                .count();

        // Get the creation date of the department as LocalDateTime
        java.time.LocalDateTime createdOn = department.getCreatedAt() != null
                ? department.getCreatedAt().toLocalDateTime()
                : java.time.LocalDateTime.now();

        // Get all permissions for this role (converted to string list)
        List<String> permissions = permissionRepository.findByRole(role)
                .stream()
                .map(permission -> {
                    // Create a readable permission string with actions
                    String actions = permission.getActions().stream()
                            .map(PermissionAction::name)
                            .collect(Collectors.joining(", "));
                    return permission.getResource().getResourceName();
                })
                .collect(Collectors.toList());

        // Determine status based on whether role has members and permissions
        String status = noOfEmployees > 0 ? "ACTIVE" : "INACTIVE";

        return new DeptRoleTable(
                department.getDepartmentId(),
                department.getDepartmentName(),
                role.getName(),
                noOfEmployees,
                createdOn,
                permissions,
                status
        );
    }
}

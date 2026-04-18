package com.nexus.iam.controller;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.dto.OrganizationDto;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.entities.User;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.security.JwtUtil;
import com.nexus.iam.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/iam/organizations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    private final UserRepository userRepository;

    private final JwtUtil jwtUtil;

    @LogActivity("Create Organization")
    @PostMapping("/add")
    public ResponseEntity<?> createOrganization(@RequestBody OrganizationDto organizationDto,
                                                @RequestParam Long member) {

        if (ObjectUtils.isEmpty(member)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Member ID is required");
        }
        if (ObjectUtils.isEmpty(organizationDto) || ObjectUtils.isEmpty(organizationDto.getOrgName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization data is required");
        }

        // Create the organization
        OrganizationDto organization = organizationService.createOrganization(organizationDto, member);

        // Fetch the updated user to get role and organization information
        User updatedUser = userRepository.findById(member).orElse(null);

        // Build comprehensive response
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("organization", organization);
        responseBody.put("orgId", organization.getId());

        // Get the first role from user's roles
        if (updatedUser != null && !updatedUser.getRoles().isEmpty()) {
            String role = updatedUser.getRoles().stream()
                    .findFirst()
                    .map(r -> "ROLE_" + r.getName())
                    .orElse("ROLE_USER");
            responseBody.put("role", role);
        } else {
            responseBody.put("role", "ROLE_DIRECTOR");
        }

        responseBody.put("message", "Organization created successfully");

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    @LogActivity("Get Organization By ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrganizationById(@PathVariable Long id) {
        if (ObjectUtils.isEmpty(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization ID is required");
        }
        return ResponseEntity.ok(organizationService.getOrganizationById(id));
    }

    @GetMapping("/details/{id}")
    public ResponseEntity<?> getOrganizationDetails(@PathVariable Long id) {
        if (ObjectUtils.isEmpty(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization ID is required");
        }
        return organizationService.getOrganizationDetailsById(id);
    }

    @LogActivity("Get All Organizations")
    @GetMapping("/")
    public ResponseEntity<?> getAllOrganizations() {
        List<Organization> organizations = organizationService.getAllOrganizations();
        return ResponseEntity.ok(organizations);
    }

    @LogActivity("Get Organization By Name")
    @GetMapping("/name/{orgName}")
    public ResponseEntity<?> getOrganizationByName(@PathVariable String orgName) {
        OrganizationDto organization = organizationService.getOrganizationByName(orgName);
        return ResponseEntity.ok(organization);
    }

    @LogActivity("Update Organization")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrganization(@PathVariable Long id, @RequestBody OrganizationDto organizationDto) {
        OrganizationDto organization = organizationService.updateOrganization(id, organizationDto);
        return ResponseEntity.ok(organization);
    }

    @LogActivity("Delete Organization")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrganization(@PathVariable Long id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @LogActivity("Get User Organization Details")
    @GetMapping(value = "/user-org/details")
    public ResponseEntity<?> getUserOrganizationDetails(@RequestParam Long userId) {
        Map<String, Object> userOrgDetails = organizationService.getUserOrganizationDetails(userId);
        return ResponseEntity.ok(userOrgDetails);
    }

    @LogActivity("Get Employee Insights")
    @GetMapping("/employees/insights")
    public ResponseEntity<?> getEmployeeInsights(@RequestParam Long orgId) {
        return organizationService.getEmployeeInsights(orgId);
    }

    @GetMapping("/employee/directory")
    public ResponseEntity<?> getEmployeeDirectory(@RequestParam Long orgId,
                                                  @RequestParam(value = "pageNo", required = false, defaultValue = "0") Integer pageNo,
                                                  @RequestParam(value = "pageOffset", required = false, defaultValue = "10") Integer pageOffset) {
        return organizationService.getEmployeeDirectory(orgId, pageNo, pageOffset);
    }

    @GetMapping("/employee/details")
    public ResponseEntity<?> getEmployeeDetails(@RequestParam Long userId) {
        return organizationService.getEmployeeDetails(userId);
    }

    @GetMapping("/employees/attendance")
    public ResponseEntity<?> getEmployeesAttendance(@RequestParam Long orgId,
                                                    @RequestParam(required = false) Long deptId,
                                                    @RequestParam String date,
                                                    @RequestParam(required = false, defaultValue = "0") Integer pageNo,
                                                    @RequestParam(required = false, defaultValue = "10") Integer pageOffset,
                                                    @RequestHeader("Authorization") String authHeader) {
        if (ObjectUtils.isEmpty(authHeader) || !jwtUtil.isValidToken(authHeader)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid or missing token");
        }
        return organizationService.getEmployeesAttendance(orgId, deptId, date, pageNo, pageOffset, authHeader);
    }

    @GetMapping("/employee/toggle-attendance")
    public ResponseEntity<?> toggleAttendance(@RequestParam Long userId,
                                              @RequestHeader("Authorization") String authHeader) {
        if (ObjectUtils.isEmpty(authHeader) || !jwtUtil.isValidToken(authHeader)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid or missing token");
        }
        return organizationService.toggleAttendance(userId, authHeader);

    }

    @GetMapping("/get-payroll-employees")
    public ResponseEntity<?> getPayrollEmployees(@RequestParam Long orgId, @RequestParam(required = false) Long deptId, @RequestParam(required = false) String role, @RequestParam(required = false, defaultValue = "0") Integer pageNo, @RequestParam(required = false, defaultValue = "10") Integer pageOffset, @RequestHeader("Authorization") String token) {
        if (ObjectUtils.isEmpty(token) || !jwtUtil.isValidToken(token)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid or missing token");
        }
        return organizationService.getPayrollEmployees(orgId, deptId, role, pageNo, pageOffset, token);
    }

    @GetMapping("/get-employee-this-month-attendance/{id}")
    public ResponseEntity<?> getEmployeeThisMonthAttendance(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        if (ObjectUtils.isEmpty(token) || !jwtUtil.isValidToken(token)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid or missing token");
        }
        return organizationService.getEmployeeThisMonthAttendance(id, token);
    }

    @GetMapping("/get-processed-payrolls")
    public ResponseEntity<?> getProcessedPayrolls(@RequestParam Long orgId, @RequestParam Integer month, @RequestParam Integer year, @RequestParam(required = false, defaultValue = "0") Integer pageNo, @RequestParam(required = false, defaultValue = "10") Integer pageSize, @RequestHeader("Authorization") String token) {
        if (ObjectUtils.isEmpty(token) || !jwtUtil.isValidToken(token)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid or missing token");
        }
        return organizationService.getProcessedPayrolls(orgId, month, year, pageNo, pageSize, token);
    }

    @GetMapping("/get-payroll-graphs")
    public ResponseEntity<?> getPayrollGraphs(@RequestParam Long orgId, @RequestParam String month, @RequestParam Integer year, @RequestHeader("Authorization") String token) {
        if (ObjectUtils.isEmpty(token) || !jwtUtil.isValidToken(token)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid or missing token");
        }
        return organizationService.getPayrollGraphs(orgId, month, year);
    }

    @GetMapping("/get-payroll-insights")
    public ResponseEntity<?> getPayrollInsights(@RequestParam Long orgId, @RequestParam String month, @RequestParam Integer year, @RequestHeader("Authorization") String token) {
        if (ObjectUtils.isEmpty(token) || !jwtUtil.isValidToken(token)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid or missing token");
        }
        return organizationService.getPayrollInsights(orgId, month, year);
    }

}

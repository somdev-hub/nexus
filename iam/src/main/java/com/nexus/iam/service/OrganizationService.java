package com.nexus.iam.service;

import com.nexus.iam.dto.OrganizationDto;
import com.nexus.iam.dto.OrganizationFetchDto;
import com.nexus.iam.entities.Organization;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface OrganizationService {
    OrganizationDto createOrganization(OrganizationDto organizationDto, Long member);

    OrganizationFetchDto getOrganizationById(Long id);

    List<Organization> getAllOrganizations();

    OrganizationDto updateOrganization(Long id, OrganizationDto organizationDto);

    void deleteOrganization(Long id);

    OrganizationDto getOrganizationByName(String orgName);

    void assignMemberToOrganization(Long orgId, Long memberId);

    void removeMemberFromOrganization(Long orgId, Long memberId);

    Map<String, Object> getUserOrganizationDetails(Long userId);

    ResponseEntity<?> getEmployeeInsights(Long orgId);

    ResponseEntity<?> getEmployeeDirectory(Long orgId, Integer pageNo, Integer pageOffset);

    ResponseEntity<?> getEmployeeDetails(Long userId);

    ResponseEntity<?> getEmployeesAttendance(Long orgId, Long deptId, String date, Integer pageNo, Integer pageOffset, String authHeader);

    ResponseEntity<?> toggleAttendance(Long userId, String authHeader);

    ResponseEntity<?> getOrganizationDetailsById(Long id);

    ResponseEntity<?> getPayrollEmployees(Long orgId, Long deptId, String role, Integer pageNo, Integer pageOffset, String token);

    ResponseEntity<?> getEmployeeThisMonthAttendance(Long id, String token);

    ResponseEntity<?> getProcessedPayrolls(Long orgId, Integer month, Integer year, Integer pageNo, Integer pageOffset, String token);

    ResponseEntity<?> getPayrollGraphs(Long orgId, String month, Integer year);

    ResponseEntity<?> getPayrollInsights(Long orgId, String month, Integer year);

    ResponseEntity<?> createHrRequest(String requestBody, String token);

    ResponseEntity<?> getManyHrRequests(Long orgId, String requestType, String status, Integer page, Integer offset, String token);

    ResponseEntity<?> takeActionOnHrRequest(Long requestId, String action, String resolutionRemarks, String token);

    ResponseEntity<?> getClosedHrRequests(Long orgId, Integer page, Integer offset, String token);

    ResponseEntity<?> getHrRequestInsights(Long orgId);
}

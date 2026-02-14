package com.nexus.iam.service.impl;

import com.nexus.iam.dto.LoginRequest;
import com.nexus.iam.dto.LoginResponse;
import com.nexus.iam.dto.OrganizationDto;
import com.nexus.iam.dto.OrganizationFetchDto;
import com.nexus.iam.dto.response.EmployeeDirectoryResponse;
import com.nexus.iam.dto.response.EmployeePageInsights;
import com.nexus.iam.dto.response.PaginatedResponse;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.entities.Role;
import com.nexus.iam.entities.User;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.repository.OrganizationRepository;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.service.AuthenticationService;
import com.nexus.iam.service.OrganizationService;
import com.nexus.iam.utils.CommonUtils;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final ModelMapper modelMapper;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final WebConstants webConstants;
    private final CommonUtils commonUtils;
    private final AuthenticationService authenticationService;
    private final RestService restService;


    @Override
    public OrganizationDto createOrganization(OrganizationDto organizationDto, Long userId) {
        if (ObjectUtils.isEmpty(organizationDto)) {
            throw new IllegalArgumentException("Organization data cannot be null or empty");
        }
        if (ObjectUtils.isEmpty(organizationDto.getOrgName())) {
            throw new IllegalArgumentException("Organization name is required");
        }
        if (Boolean.TRUE.equals(organizationRepository.existsByOrgName(organizationDto.getOrgName()))) {
            throw new IllegalArgumentException(
                    "Organization with name already exists: " + organizationDto.getOrgName());
        }

        // Get the user - fail early if user doesn't exist
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Verify DIRECTOR role exists - fail early if role doesn't exist
        var directorRole = roleRepository.findByName("DIRECTOR")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "DIRECTOR"));

        // Create the Organization only after all validations pass
        Organization organization = modelMapper.map(organizationDto, Organization.class);
        organization.setUsers(new ArrayList<>());
        organization.setDocuments(new ArrayList<>());
        organization.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        Organization savedOrganization = organizationRepository.save(organization);

        // Assign user to this organization with DIRECTOR role
        user.setOrganization(savedOrganization);
        user.getRoles().add(directorRole);

        userRepository.save(user);

        return modelMapper.map(savedOrganization, OrganizationDto.class);
    }

    @Override
    public OrganizationFetchDto getOrganizationById(Long id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        return organizationRepository.fetchByOrgId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
    }

    @Override
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    @Override
    public OrganizationDto updateOrganization(Long id, OrganizationDto organizationDto) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        if (ObjectUtils.isEmpty(organizationDto)) {
            throw new IllegalArgumentException("Organization data cannot be null or empty");
        }

        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));

        if (Boolean.TRUE.equals(organizationRepository.existsByOrgName(organizationDto.getOrgName())) && !ObjectUtils.isEmpty(organizationDto.getOrgName()) && !organizationDto.getOrgName().equals(organization.getOrgName())) {
            throw new IllegalArgumentException(
                    "Organization with name already exists: " + organizationDto.getOrgName());
        }

        modelMapper.map(organizationDto, organization);
        Organization updatedOrganization = organizationRepository.save(organization);
        return modelMapper.map(updatedOrganization, OrganizationDto.class);
    }

    @Override
    public void deleteOrganization(Long id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
        organizationRepository.delete(organization);
    }

    @Override
    public OrganizationDto getOrganizationByName(String orgName) {
        if (ObjectUtils.isEmpty(orgName)) {
            throw new IllegalArgumentException("Organization name cannot be null or empty");
        }
        Organization organization = organizationRepository.findByOrgName(orgName)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "name", orgName));
        return modelMapper.map(organization, OrganizationDto.class);
    }

    @Override
    public void assignMemberToOrganization(Long orgId, Long userId) {
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Set organization for the user
        user.setOrganization(organization);

        // Add MEMBER role if not already present
        user.getRoles().add(roleRepository.findByName("MEMBER")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "MEMBER")));

        userRepository.save(user);
    }

    @Override
    public void removeMemberFromOrganization(Long orgId, Long userId) {
        // Verify organization exists
        organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Remove organization from user
        if (user.getOrganization() != null && user.getOrganization().getId().equals(orgId)) {
            user.setOrganization(null);
            userRepository.save(user);
        }
    }

    @Override
    public Map<String, Object> getUserOrganizationDetails(Long userId) {
        try {
            Map<String, Object> result = new HashMap<>();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            Organization organization = user.getOrganization();
            if (organization == null) {
                throw new ResourceNotFoundException("Organization", "userId", userId);
            }
            result.put("orgName", organization.getOrgName());
            result.put("orgId", organization.getId());
            result.put("userRoles", user.getRoles().stream().map(Role::getName).toList());
            result.put("orgType", organization.getOrgType());
            return result;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "OrganizationService", "Failed to get user organization details", "getUserOrganizationDetails",
                    e.getClass().getSimpleName(), e.getLocalizedMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getEmployeeInsights(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        ResponseEntity<?> response;
        Integer totalEmployees, totalDepartments, onNoticePeriod = 0;
        Map<String, Integer> employeesPerDepartment = new HashMap<>(), genderRatio = new HashMap<>();

        try {
            Organization organization = organizationRepository.findById(orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

            totalEmployees = organization.getUsers().size();
            totalDepartments = organization.getDepartments().size();

            for (var department : organization.getDepartments()) {
                employeesPerDepartment.put(department.getDepartmentName(), department.getMembers().size());
            }

            for (var user : organization.getUsers()) {
                String gender = user.getGender() != null ? user.getGender().name() : "Unknown";
                genderRatio.put(gender, genderRatio.getOrDefault(gender, 0) + 1);
            }

            // fetch notice period count
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getEmployeeOnNoticePeriodUrl()).queryParam("orgId", orgId);
            LoginResponse loginResponse = authenticationService.authenticate(new LoginRequest(webConstants.getGenericUserId(), webConstants.getGenericPassword()));
            Map<String, String> headers = commonUtils.buildJsonHeaders(loginResponse.getAccessToken());
            ResponseEntity<?> apiResponse = restService.iamRestCall(
                    builder.toUriString(),
                    null,
                    headers,
                    HttpMethod.GET,
                    null
            );
            if (apiResponse.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked") Map<String, Object> body = (Map<String, Object>) apiResponse.getBody();
                if (body != null && body.containsKey("onNoticePeriodCount")) {
                    onNoticePeriod = (Integer) body.get("allWhoAreOnNoticePeriod");
                }
            }

            response = ResponseEntity.ok(new EmployeePageInsights(
                    totalEmployees, totalDepartments, employeesPerDepartment, genderRatio, onNoticePeriod
            ));
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationService", "Failed to get employee insights", "getEmployeeInsights",
                    e.getClass().getSimpleName(), e.getLocalizedMessage()
            );
        }

        return response;


    }

    @Override
    public ResponseEntity<?> getEmployeeDirectory(Long orgId, Integer pageNo, Integer pageOffset) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }

        try {
            // 1. Validate organization exists
            organizationRepository.findById(orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

            // 2. Fetch only users from this organization
            Pageable pageable = PageRequest.of(pageNo, pageOffset);
            Organization organization = organizationRepository.findById(orgId).orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));
            Page<User> users = userRepository.findByOrganization(organization, pageable);

            if (users.isEmpty()) {
                return ResponseEntity.ok(new PaginatedResponse<>(
                        new ArrayList<>(),
                        pageNo,
                        pageOffset,
                        0L,
                        0,
                        true,
                        true,
                        false,
                        false
                ));
            }

            // 3. Create lookup map for O(1) access instead of O(n) search
            Map<Long, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));

            List<Long> userIds = new ArrayList<>(userMap.keySet());

            // 4. Fetch employee details
            LoginResponse loginResponse = authenticationService.authenticate(
                    new LoginRequest(webConstants.getGenericUserId(), webConstants.getGenericPassword())
            );
            Map<String, String> headers = commonUtils.buildJsonHeaders(loginResponse.getAccessToken());

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getEmployeeDirectoryUrl());
            ResponseEntity<?> apiResponse = restService.iamRestCall(
                    builder.toUriString(), userIds, headers, HttpMethod.POST, null
            );

            // 5. Process response safely
            if (!apiResponse.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "OrganizationService", "Failed to fetch employee details", "getEmployeeDirectory",
                        "API_ERROR", "External API returned status: " + apiResponse.getStatusCode()
                );
            }

            @SuppressWarnings("unchecked") List<Map<String, Object>> employeeDetails = (List<Map<String, Object>>) apiResponse.getBody();
            if (employeeDetails == null) {
                return ResponseEntity.ok(new PaginatedResponse<>(
                        new ArrayList<>(),
                        pageNo,
                        pageOffset,
                        users.getTotalElements(),
                        users.getTotalPages(),
                        users.isFirst(),
                        users.isLast(),
                        users.hasNext(),
                        users.hasPrevious()
                ));
            }

            // 6. Combine data using map lookup
            List<EmployeeDirectoryResponse> result = employeeDetails.stream()
                    .map(detail -> {
                        Long empId = extractLong(detail, "empId");
                        User user = userMap.get(empId);

                        return new EmployeeDirectoryResponse(
                                empId,
                                user != null ? user.getName() : "",
                                user != null ? user.getEmail() : "",
                                extractString(detail, "deptName"),
                                extractString(detail, "position"),
                                extractDouble(detail, "salary"),
                                extractDate(detail, "joiningDate")
                        );
                    })
                    .toList();

            // 7. Return paginated response with metadata
            return ResponseEntity.ok(new PaginatedResponse<>(
                    result,
                    pageNo,
                    pageOffset,
                    users.getTotalElements(),
                    users.getTotalPages(),
                    users.isFirst(),
                    users.isLast(),
                    users.hasNext(),
                    users.hasPrevious()
            ));

        } catch (ResourceNotFoundException | ServiceLevelException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationService", "Failed to get employee directory", "getEmployeeDirectory",
                    e.getClass().getSimpleName(), e.getLocalizedMessage()
            );
        }
    }

    private Long extractLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? Long.valueOf(value.toString()) : null;
    }

    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private Double extractDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? Double.valueOf(value.toString()) : 0.0;
    }

    private LocalDateTime extractDate(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }

        String dateString = value.toString();
        if (dateString.contains("T")){
            dateString=dateString.substring(0,dateString.indexOf("T"));
        }
        return LocalDateTime.of(Date.valueOf(dateString).toLocalDate(), LocalDateTime.MIN.toLocalTime());
    }
}

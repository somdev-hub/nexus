package com.nexus.iam.service.impl;

import com.nexus.iam.config.CacheConfig;
import com.nexus.iam.dto.LoginResponse;
import com.nexus.iam.dto.OrganizationDto;
import com.nexus.iam.dto.OrganizationFetchDto;
import com.nexus.iam.dto.response.*;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.entities.Role;
import com.nexus.iam.entities.User;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.repository.OrganizationRepository;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.service.AuthenticationService;
import com.nexus.iam.service.KeycloakAuthenticationService;
import com.nexus.iam.service.OrganizationService;
import com.nexus.iam.utils.CommonUtils;
import com.nexus.iam.utils.DataMapper;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

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
    private final KeycloakAuthenticationService keycloakAuthenticationService;

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
    @Cacheable(value = CacheConfig.ORGANIZATION_CACHE, key = "#id")
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

        if (Boolean.TRUE.equals(organizationRepository.existsByOrgName(organizationDto.getOrgName()))
                && !ObjectUtils.isEmpty(organizationDto.getOrgName())
                && !organizationDto.getOrgName().equals(organization.getOrgName())) {
            throw new IllegalArgumentException(
                    "Organization with name already exists: " + organizationDto.getOrgName());
        }
        Timestamp createdAt = organization.getCreatedAt();
        modelMapper.map(organizationDto, organization);
        organization.setCreatedAt(createdAt);
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
    @Cacheable(value = CacheConfig.USER_ORGANIZATION_DETAILS_CACHE, key = "#userId")
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
                    e.getClass().getSimpleName(), e.getLocalizedMessage());
        }
    }

    @Override
    @Cacheable(value = CacheConfig.EMPLOYEE_INSIGHTS_CACHE, key = "#orgId")
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
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(webConstants.getEmployeeOnNoticePeriodUrl()).queryParam("orgId", orgId);
            ResponseEntity<LoginResponse> loginResponseEntity = keycloakAuthenticationService
                    .login(webConstants.getGenericUserId(), webConstants.getGenericPassword());
            LoginResponse loginResponse = loginResponseEntity.getBody();
            Map<String, String> headers = commonUtils.buildJsonHeaders(loginResponse.getAccessToken());
            ResponseEntity<?> apiResponse = restService.iamRestCall(
                    builder.toUriString(),
                    null,
                    headers,
                    HttpMethod.GET,
                    null);
            if (apiResponse.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = (Map<String, Object>) apiResponse.getBody();
                if (body != null && body.containsKey("onNoticePeriodCount")) {
                    onNoticePeriod = (Integer) body.get("allWhoAreOnNoticePeriod");
                }
            }

            response = ResponseEntity.ok(new EmployeePageInsights(
                    totalEmployees, totalDepartments, employeesPerDepartment, genderRatio, onNoticePeriod));
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationService", "Failed to get employee insights", "getEmployeeInsights",
                    e.getClass().getSimpleName(), e.getLocalizedMessage());
        }

        return response;

    }

    @Override
    @Cacheable(value = CacheConfig.EMPLOYEE_DIRECTORY_CACHE, key = "#orgId + '-' + #pageNo + '-' + #pageOffset")
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
            Organization organization = organizationRepository.findById(orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));
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
                        false));
            }

            // 3. Create lookup map for O(1) access instead of O(n) search
            Map<Long, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));

            List<Long> userIds = new ArrayList<>(userMap.keySet());

            // 4. Fetch employee details
            ResponseEntity<LoginResponse> loginResponseEntity = keycloakAuthenticationService.login(
                    webConstants.getGenericUserId(), webConstants.getGenericPassword());
            LoginResponse loginResponse = loginResponseEntity.getBody();
            Map<String, String> headers = commonUtils.buildJsonHeaders(loginResponse.getAccessToken());

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getEmployeeDirectoryUrl());
            ResponseEntity<?> apiResponse = restService.iamRestCall(
                    builder.toUriString(), userIds, headers, HttpMethod.POST, null);

            // 5. Process response safely
            if (!apiResponse.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "OrganizationService", "Failed to fetch employee details", "getEmployeeDirectory",
                        "API_ERROR", "External API returned status: " + apiResponse.getStatusCode());
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> employeeDetails = (List<Map<String, Object>>) apiResponse.getBody();
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
                        users.hasPrevious()));
            }

            // 6. Combine data using map lookup
            List<EmployeeDirectoryResponse> result = employeeDetails.stream()
                    .map(detail -> {
                        Long empId = DataMapper.extractField(detail, "empId", Long.class);
                        User user = userMap.get(empId);
                        LocalDateTime joiningDate = DataMapper.extractField(detail, "joiningDate", LocalDateTime.class);

                        return new EmployeeDirectoryResponse(
                                empId,
                                user != null ? user.getName() : "",
                                user != null ? user.getEmail() : "",
                                DataMapper.extractField(detail, "deptName", String.class),
                                DataMapper.extractField(detail, "position", String.class),
                                DataMapper.extractField(detail, "salary", Double.class),
                                joiningDate);
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
                    users.hasPrevious()));

        } catch (ResourceNotFoundException | ServiceLevelException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationService", "Failed to get employee directory", "getEmployeeDirectory",
                    e.getClass().getSimpleName(), e.getLocalizedMessage());
        }
    }

    @Override
    @Cacheable(value = CacheConfig.EMPLOYEE_DETAILS_CACHE, key = "#userId")
    public ResponseEntity<?> getEmployeeDetails(Long userId) {
        if (ObjectUtils.isEmpty(userId)) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        ResponseEntity<?> response = null;
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            EmployeeProfileResponse employeeProfileResponse = new EmployeeProfileResponse();
            employeeProfileResponse.setEmpId(user.getId());
            employeeProfileResponse.setFullName(user.getName());
            employeeProfileResponse.setEmail(user.getEmail());
            employeeProfileResponse.setGender(user.getGender());
            employeeProfileResponse.setPhone(user.getPhone());
            employeeProfileResponse.setAddress(user.getAddress());
            employeeProfileResponse.setAge(user.getAge());
            employeeProfileResponse.setProfileImageUrl(user.getProfilePhoto());

            // for other details contact HR microservice
            ResponseEntity<LoginResponse> loginResponseEntity = keycloakAuthenticationService.login(
                    webConstants.getGenericUserId(), webConstants.getGenericPassword());
            LoginResponse loginResponse = loginResponseEntity.getBody();
            Map<String, String> headers = commonUtils.buildJsonHeaders(loginResponse.getAccessToken());
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getEmployeeDetailsUrl())
                    .queryParam("empId", user.getId());
            ResponseEntity<?> hrResponse = restService.iamRestCall(
                    builder.toUriString(),
                    null,
                    headers,
                    HttpMethod.GET,
                    null);
            if (hrResponse.getStatusCode().is2xxSuccessful() && hrResponse.getBody() != null) {
                // Get the response body as Map
                @SuppressWarnings("unchecked")
                Map<String, Object> responseData = (Map<String, Object>) hrResponse.getBody();

                // Create a new map with the HR data plus IAM service user details
                Map<String, Object> combinedData = new HashMap<>(responseData);

                // Add IAM service user details
                combinedData.put("empId", user.getId());
                combinedData.put("fullName", user.getName());
                combinedData.put("email", user.getEmail());
                combinedData.put("gender", user.getGender());
                combinedData.put("phone", user.getPhone());
                combinedData.put("address", user.getAddress());
                combinedData.put("age", user.getAge());
                combinedData.put("profileImageUrl", user.getProfilePhoto());

                // Return the combined data directly - Spring will serialize it properly
                response = ResponseEntity.ok(combinedData);

                // @SuppressWarnings("unchecked") Map<String, Object> hrDetails = (Map<String,
                // Object>) hrResponse.getBody();
                // if (hrDetails != null) {
                // // Map basic fields
                // employeeProfileResponse.setDepartment(DataMapper.extractField(hrDetails,
                // "department", String.class));
                // employeeProfileResponse.setJobTitle(DataMapper.extractField(hrDetails,
                // "jobTitle", String.class));
                // employeeProfileResponse.setAnnualSalary(DataMapper.extractField(hrDetails,
                // "annualSalary", Double.class));
                // employeeProfileResponse.setJoiningDate(DataMapper.extractField(hrDetails,
                // "joiningDate", Date.class));
                // employeeProfileResponse.setCoverImageUrl(DataMapper.extractField(hrDetails,
                // "coverImageUrl", String.class));
                // employeeProfileResponse.setProfileImageUrl(DataMapper.extractField(hrDetails,
                // "profileImageUrl", String.class));
                //
                // // Map compensation details with automatic conversion of nested objects and
                // lists
                // Map<String, Object> compensationData = DataMapper.extractMapField(hrDetails,
                // "compensation");
                // if (!compensationData.isEmpty()) {
                // CompensationDto compensationDto = DataMapper.mapToObject(compensationData,
                // CompensationDto.class);
                // // Map nested lists within compensation
                // compensationDto.setBonuses(DataMapper.extractListField(compensationData,
                // "bonuses", BonusDto.class));
                // compensationDto.setDeductions(DataMapper.extractListField(compensationData,
                // "deductions", DeductionDto.class));
                // compensationDto.setBankRecords(DataMapper.extractListField(compensationData,
                // "bankRecords", BankRecordsDto.class));
                // employeeProfileResponse.setCompensation(compensationDto);
                // }
                //
                // // Map leave records
                // List<Map<String, Object>> leaveRecordsList =
                // DataMapper.extractListOfMaps(hrDetails, "leaveRecords");
                // List<EmployeeProfileResponse.LeaveRecord> leaveRecords =
                // leaveRecordsList.stream()
                // .map(record -> new EmployeeProfileResponse.LeaveRecord(
                // DataMapper.extractField(record, "leaveType", String.class),
                // DataMapper.extractField(record, "totalLeaves", Double.class),
                // DataMapper.extractField(record, "leavesTaken", Double.class),
                // DataMapper.extractField(record, "remainingLeaves", Double.class)
                // ))
                // .toList();
                // employeeProfileResponse.setLeaveRecords(leaveRecords);
                //
                // // Map attendance records
                // List<Map<String, Object>> attendanceList =
                // DataMapper.extractListOfMaps(hrDetails, "attendanceRecords");
                // List<EmployeeProfileResponse.AttendanceRecord> attendanceRecords =
                // attendanceList.stream()
                // .map(record -> new EmployeeProfileResponse.AttendanceRecord(
                // DataMapper.extractField(record, "date", LocalDateTime.class),
                // DataMapper.extractField(record, "status", String.class),
                // DataMapper.extractField(record, "checkInTime", LocalDateTime.class),
                // DataMapper.extractField(record, "checkOutTime", LocalDateTime.class),
                // DataMapper.extractField(record, "hoursWorked", Double.class),
                // DataMapper.extractField(record, "breakHours", Double.class),
                // DataMapper.extractField(record, "overtimeHours", Double.class)
                // ))
                // .toList();
                // employeeProfileResponse.setAttendanceRecords(attendanceRecords);
                //
                // // Map positions held
                // List<Map<String, Object>> positionsList =
                // DataMapper.extractListOfMaps(hrDetails, "positionsHeld");
                // List<EmployeeProfileResponse.PositionsHeld> positionsHeld =
                // positionsList.stream()
                // .map(position -> new EmployeeProfileResponse.PositionsHeld(
                // DataMapper.extractField(position, "title", String.class),
                // DataMapper.extractField(position, "department", String.class),
                // DataMapper.extractField(position, "fromDate", LocalDateTime.class),
                // DataMapper.extractField(position, "toDate", LocalDateTime.class),
                // DataMapper.extractField(position, "duration", Double.class)
                // ))
                // .toList();
                // employeeProfileResponse.setPositionsHeld(positionsHeld);
                //
                // // Map HR documents
                // List<Map<String, Object>> documentsList =
                // DataMapper.extractListOfMaps(hrDetails, "hrDocuments");
                // List<EmployeeProfileResponse.HrDocuments> hrDocuments =
                // documentsList.stream()
                // .map(doc -> new EmployeeProfileResponse.HrDocuments(
                // DataMapper.extractField(doc, "documentName", String.class),
                // DataMapper.extractField(doc, "documentUrl", String.class),
                // DataMapper.extractField(doc, "uploadedOn", Timestamp.class),
                // DataMapper.extractField(doc, "documentType", String.class)
                // ))
                // .toList();
                // employeeProfileResponse.setHrDocuments(hrDocuments);
                // }
                // response = ResponseEntity.ok(jsonObject);
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationService", "Failed to get employee details", "getEmployeeDetails",
                    e.getClass().getSimpleName(), e.getLocalizedMessage());
        }
        return response;
    }

    @Override
    public ResponseEntity<?> getEmployeesAttendance(Long orgId, Long deptId, String date, Integer pageNo, Integer pageOffset,
                                                    String authHeader) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new IllegalArgumentException("Organization ID is required");
        }

        Pageable pageable = PageRequest.of(pageNo, pageOffset);
        try {
            // 1. Fetch users from database
            Page<User> users;
            if (ObjectUtils.isEmpty(deptId)) {
                // Fetch attendance for all departments in the organization
                users = userRepository.findAll(pageable);
            } else {
                // Fetch attendance for a specific department
                users = userRepository.findByDepartmentId(deptId, pageable);
            }

            if (users.isEmpty()) {
                return ResponseEntity.ok(new PageImpl<>(
                        List.of(),
                        pageable,
                        0));
            }

            // 2. Create a lookup map for O(1) access to user names
            Map<Long, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));

            // 3. Extract employee IDs
            List<Long> empIds = new ArrayList<>(userMap.keySet());

            // 4. Call HR service to get attendance data
            Map<String, String> headers = new HashMap<>(commonUtils.buildJsonHeaders(authHeader));
            headers.put(HttpHeaders.AUTHORIZATION, authHeader);
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getEmployeeAttendanceUrl());
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("empIds", empIds);
            requestBody.put("date", date);
            ResponseEntity<?> hrCallResponse = restService.iamRestCall(
                    builder.toUriString(),
                    requestBody,
                    headers,
                    HttpMethod.POST,
                    null);

            // 5. Process HR response
            if (!hrCallResponse.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(hrCallResponse.getStatusCode())
                        .body("Failed to fetch attendance from HR service");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attendanceDataList = (List<Map<String, Object>>) hrCallResponse.getBody();

            if (attendanceDataList == null || attendanceDataList.isEmpty()) {
                return ResponseEntity.ok(new PageImpl<>(
                        List.of(),
                        pageable,
                        users.getTotalElements()));
            }

            // 6. Map attendance data to EmployeesAttendanceDto with employee names
            List<EmployeesAttendanceDto> attendanceResponses = attendanceDataList.stream()
                    .map(attendanceData -> {
                        Long empId = ((Number) attendanceData.get("employeeId")).longValue();
                        User user = userMap.get(empId);

                        EmployeesAttendanceDto dto = new EmployeesAttendanceDto();
                        dto.setDate(attendanceData.get("date") != null
                                ? java.time.LocalDate.parse(attendanceData.get("date").toString())
                                : null);
                        dto.setEmployeeId(empId);
                        dto.setEmployeeName(user != null ? user.getName() : "");
                        dto.setCheckInTime(attendanceData.get("checkInTime") != null ? attendanceData.get("checkInTime").toString() : "");
                        dto.setCheckOutTime(attendanceData.get("checkOutTime") != null ? attendanceData.get("checkOutTime").toString() : "");
                        dto.setTotalHoursWorked(attendanceData.get("totalHoursWorked") != null
                                ? ((Number) attendanceData.get("totalHoursWorked")).doubleValue()
                                : 0.0);
                        dto.setStatus((String) attendanceData.get("status"));

                        return dto;
                    })
                    .toList();

            // 7. Return paginated response
            return ResponseEntity.ok(new PageImpl<>(
                    attendanceResponses,
                    pageable,
                    users.getTotalElements()));

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "DepartmentServiceImpl",
                    "Failed to get employees attendance: " + e.getMessage(),
                    "getEmployeesAttendance",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage());
        }
    }

    @Override
    public ResponseEntity<?> toggleAttendance(Long userId, String authHeader) {
        if (ObjectUtils.isEmpty(userId)) {
            throw new IllegalArgumentException("User ID is required");
        }

        try {
            // 1. Validate user exists
            if (!userRepository.existsById(userId)) {
                throw new ResourceNotFoundException("User", "id", userId);
            }

            // 2. Call HR service to toggle attendance
            Map<String, String> headers = new HashMap<>(commonUtils.buildJsonHeaders(authHeader));
            headers.put(HttpHeaders.AUTHORIZATION, authHeader);
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getToggleAttendanceUrl())
                    .queryParam("empId", userId);
            ResponseEntity<?> hrCallResponse = restService.iamRestCall(
                    builder.toUriString(),
                    null,
                    headers,
                    HttpMethod.POST,
                    null);

            // 3. Process HR response
            if (!hrCallResponse.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(hrCallResponse.getStatusCode())
                        .body("Failed to toggle attendance in HR service");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> responseData = (Map<String, Object>) hrCallResponse.getBody();
            return ResponseEntity.ok(responseData);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Failed to toggle attendance: " + e.getMessage(),
                    "toggleAttendance",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage());
        }
    }

    @Override
    public ResponseEntity<?> getOrganizationDetailsById(Long id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        try {
            Organization organization = organizationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
            OrganizationDto organizationDto = modelMapper.map(organization, OrganizationDto.class);
            return ResponseEntity.ok(organizationDto);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Failed to get organization details: " + e.getMessage(),
                    "getOrganizationDetailsById",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage());
        }
    }

}

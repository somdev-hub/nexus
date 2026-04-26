package com.nexus.iam.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.iam.config.CacheConfig;
import com.nexus.iam.dto.LoginResponse;
import com.nexus.iam.dto.OrganizationDto;
import com.nexus.iam.dto.OrganizationFetchDto;
import com.nexus.iam.dto.response.*;
import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.entities.Role;
import com.nexus.iam.entities.User;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.repository.DepartmentRepository;
import com.nexus.iam.repository.OrganizationRepository;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.service.AuthenticationService;
import com.nexus.iam.service.KeycloakAuthenticationService;
import com.nexus.iam.service.OrganizationService;
import com.nexus.iam.utils.*;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final DepartmentRepository departmentRepository;
    private final ObjectMapper objectMapper;
    private final KeycloakTokenUtil keycloakTokenUtil;

    private static @NonNull JSONArray getJsonArray(ResponseEntity<?> response) {
        JSONArray responseData;
        Object responseBody = response.getBody();

        if (responseBody instanceof String json) {
            responseData = new JSONArray(json);
        } else if (responseBody instanceof Collection<?> collection) {
            responseData = new JSONArray(collection);
        } else if (responseBody != null && responseBody.getClass().isArray()) {
            responseData = new JSONArray(responseBody);
        } else if (responseBody instanceof JSONArray jsonArray) {
            responseData = jsonArray;
        } else {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Unsupported payroll response body type",
                    "getPayrollEmployees",
                    "INVALID_RESPONSE",
                    responseBody == null ? "Response body is null" : responseBody.getClass().getName());
        }
        return responseData;
    }

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

            // Handle various response types (String, List, JSONArray, etc.)
            List<Map<String, Object>> employeeDetails = new ArrayList<>();
            Object responseBody = apiResponse.getBody();

            if (responseBody != null) {
                if (responseBody instanceof String stringBody) {
                    // Parse string response - could be empty array "[]" or JSON array
                    try {
                        JSONArray jsonArray = new JSONArray(stringBody);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            employeeDetails.add(jsonArray.getJSONObject(i).toMap());
                        }
                    } catch (Exception e) {
                        // If parsing fails, treat as empty list
                        employeeDetails = new ArrayList<>();
                    }
                } else if (responseBody instanceof Collection<?> collection) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> listBody = (List<Map<String, Object>>) responseBody;
                    employeeDetails = listBody;
                }
            }

            if (employeeDetails.isEmpty()) {
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
//                @SuppressWarnings("unchecked")
//                Map<String, Object> responseData = (Map<String, Object>) hrResponse.getBody();
                Map<String, Object> combinedData = objectMapper.readValue(hrResponse.getBody().toString(), new TypeReference<>() {
                });
//                JSONObject combinedData= new JSONObject(hrResponse.getBody().toString());

                // Create a new map with the HR data plus IAM service user details
//                Map<String, Object> combinedData = new HashMap<>((Map) responseData);

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
        } catch (RuntimeException | JsonProcessingException e) {
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
            // Validate organization exists first
            Organization organization = organizationRepository.findById(orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

            // 1. Fetch users from database
            Page<User> users;
            if (ObjectUtils.isEmpty(deptId)) {
                // Fetch attendance for all employees in the organization
                users = userRepository.findByOrganization(organization, pageable);
            } else {
                // Validate department exists and belongs to this organization
                Department department = departmentRepository.findById(deptId)
                        .orElseThrow(() -> new ResourceNotFoundException("Department", "id", deptId));

                if (!department.getOrganization().getId().equals(orgId)) {
                    throw new IllegalArgumentException("Department does not belong to this organization");
                }

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

            // Handle various response types (String, List, JSONArray, etc.)
            List<Map<String, Object>> attendanceDataList = new ArrayList<>();
            Object responseBody = hrCallResponse.getBody();

            if (responseBody != null) {
                if (responseBody instanceof String stringBody) {
                    // Parse string response - could be empty array "[]" or JSON array
                    try {
                        JSONArray jsonArray = new JSONArray(stringBody);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            attendanceDataList.add(jsonArray.getJSONObject(i).toMap());
                        }
                    } catch (Exception e) {
                        // If parsing fails, treat as empty list
                        attendanceDataList = new ArrayList<>();
                    }
                } else if (responseBody instanceof Collection<?> collection) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> listBody = (List<Map<String, Object>>) responseBody;
                    attendanceDataList = listBody;
                } else if (responseBody instanceof List<?>) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> listBody = (List<Map<String, Object>>) responseBody;
                    attendanceDataList = listBody;
                }
            }

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

            return hrCallResponse;

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

    @Override
    public ResponseEntity<?> getPayrollEmployees(Long orgId, Long deptId, String role, Integer pageNo, Integer pageOffset, String token) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        try {
            // Validate organization exists
            Organization organization = organizationRepository.findById(orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

            Page<User> employees;
            Pageable pageable = PageRequest.of(pageNo, pageOffset);
            // Fetch employees based on provided filters
            if (!ObjectUtils.isEmpty(role) && !ObjectUtils.isEmpty(deptId)) {
                // Fetch employees by organization, department and role
                Department department = departmentRepository.findById(deptId)
                        .orElseThrow(() -> new ResourceNotFoundException("Department", "id", deptId));

                if (!department.getOrganization().getId().equals(orgId)) {
                    throw new IllegalArgumentException("Department does not belong to this organization");
                }

                Role roleEntity = roleRepository.findByName(role)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", role));
                employees = userRepository.findByDepartmentAndRole(department, roleEntity, pageable);
            } else if (!ObjectUtils.isEmpty(deptId)) {
                // Fetch employees by organization and department
                Department department = departmentRepository.findById(deptId)
                        .orElseThrow(() -> new ResourceNotFoundException("Department", "id", deptId));

                if (!department.getOrganization().getId().equals(orgId)) {
                    throw new IllegalArgumentException("Department does not belong to this organization");
                }

                employees = userRepository.findByDepartmentId(deptId, pageable);
            } else {
                // Fetch all employees in the organization
                employees = userRepository.findByOrganizationIdWithPagination(orgId, pageable);
            }

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getPayrollEmployeesUrl());
            Map<String, String> headers = commonUtils.buildJsonHeaders(token);
            List<Long> employeeIds = employees.stream().map(User::getId).toList();

            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), employeeIds, headers, HttpMethod.POST, null);
            if (!response.getStatusCode().is2xxSuccessful() || ObjectUtils.isEmpty(response.getBody())) {
                throw new ServiceLevelException(
                        "OrganizationServiceImpl",
                        "Failed to fetch payroll employees from HR service",
                        "getPayrollEmployees",
                        "API_ERROR",
                        "External API returned status: " + response.getStatusCode());
            }
            JSONArray responseData = getJsonArray(response);
            List<Map<String, Object>> map = new ArrayList<>();
            for (int i = 0; i < responseData.length(); i++) {
                map.add(responseData.getJSONObject(i).toMap());
            }
            map.forEach(data -> {
                Long employeeId = ((Number) data.get("employeeId")).longValue();
                User user = userRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException("User", "id", employeeId));
                data.put("name", user.getName());
            });


            // return paginated response of list
            return ResponseEntity.ok(new PaginatedResponse<>(
                    map,
                    pageNo,
                    pageOffset,
                    employees.getTotalElements(),
                    employees.getTotalPages(),
                    employees.isFirst(),
                    employees.isLast(),
                    employees.hasNext(),
                    employees.hasPrevious()));

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Failed to get payroll employees: " + e.getMessage(),
                    "getPayrollEmployees",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage());
        }
    }

    @Override
    public ResponseEntity<?> getEmployeeThisMonthAttendance(Long id, String token) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        try {
            // Validate user exists
            boolean exists = userRepository.existsById(id);
            if (!exists) {
                throw new ResourceNotFoundException("User", "id", id);
            }

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getEmployeeThisMonthAttendanceUrl());
            Map<String, String> headers = commonUtils.buildJsonHeaders(token);
            return restService.iamRestCall(builder.toUriString() + "/" + id, null, headers, HttpMethod.GET, id);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Failed to get employee this month attendance: " + e.getMessage(),
                    "getEmployeeThisMonthAttendance",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage());

        }
    }

    @Override
    public ResponseEntity<?> getProcessedPayrolls(Long orgId, Integer month, Integer year, Integer pageNo, Integer pageOffset, String token) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        try {
            // Validate organization exists
            if (!organizationRepository.existsById(orgId)) {
                throw new ResourceNotFoundException("Organization", "id", orgId);
            }


            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getProcessedPayrollsUrl())
                    .queryParam("orgId", orgId)
                    .queryParam("month", month)
                    .queryParam("year", year)
                    .queryParam("pageNo", pageNo)
                    .queryParam("pageSize", pageOffset);
            Map<String, String> headers = commonUtils.buildJsonHeaders(token);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), null, headers, HttpMethod.GET, null);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "OrganizationServiceImpl",
                        "Failed to fetch processed payrolls from HR service",
                        "getProcessedPayrolls",
                        "API_ERROR",
                        response.getBody() != null ? response.getBody().toString() : "External API returned status: " + response.getStatusCode());
            }
            JSONArray responseData = getJsonArray(response);
            List<Map<String, Object>> map = new ArrayList<>();
            for (int i = 0; i < responseData.length(); i++) {
                map.add(responseData.getJSONObject(i).toMap());
            }
            map.forEach(data -> {
                Long employeeId = ((Number) data.get("empId")).longValue();
                User user = userRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException("User", "id", employeeId));
                data.put("name", user.getName());
                data.put("department", user.getHeadedDepartments().stream().findFirst().map(Department::getDepartmentName).orElse(user.getMemberOfDepartments().stream().findFirst().map(Department::getDepartmentName).orElse("")));
            });

            return ResponseEntity.ok(new PaginatedResponse<Map<String, Object>>(
                    map,
                    pageNo,
                    pageOffset,
                    (long) responseData.length(),
                    (int) Math.ceil((double) responseData.length() / pageOffset),
                    pageNo == 0,
                    (pageNo + 1) * pageOffset >= responseData.length(),
                    (pageNo + 1) * pageOffset < responseData.length(),
                    pageNo > 0
            ));

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Failed to get processed payrolls: " + e.getMessage(),
                    "getProcessedPayrolls",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage());
        }

    }

    @Override
    public ResponseEntity<?> getPayrollGraphs(Long orgId, String month, Integer year) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        if (ObjectUtils.isEmpty(month) || ObjectUtils.isEmpty(year)) {
            throw new IllegalArgumentException("Month and Year are required");
        }
        try {
            List<Map<String, Object>> userIdsWithRoles = userRepository.getRolesWithUserIds(orgId);

            // Transform flat list into grouped structure: role -> list of user IDs
            Map<String, List<Long>> roleEmpIdMap = new HashMap<>();
            for (Map<String, Object> record : userIdsWithRoles) {
                String roleName = (String) record.get("roleName");
                Object userIdObj = record.get("userId");

                // Only add users that have a userId (skip roles without users)
                if (userIdObj != null) {
                    Long userId = ((Number) userIdObj).longValue();
                    roleEmpIdMap.computeIfAbsent(roleName, k -> new ArrayList<>()).add(userId);
                }
            }

            // Convert to desired format
            List<Map<String, Object>> transformedData = roleEmpIdMap.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("role", entry.getKey());
                        map.put("empIds", entry.getValue());
                        return map;
                    })
                    .toList();

            Map<String, Object> payload = new ConcurrentHashMap<>();
            payload.put("orgId", orgId);
            payload.put("month", month);
            payload.put("year", year);
            payload.put("roleEmpIdMap", transformedData);
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getPayrollGraphsUrl());

            ResponseEntity<?> response = restService.iamRestCall(
                    builder.toUriString(),
                    payload,
                    null,
                    HttpMethod.POST,
                    null
            );

            // Check if the response is successful
            if (!response.getStatusCode().is2xxSuccessful() || ObjectUtils.isEmpty(response.getBody())) {
                throw new ServiceLevelException(
                        "OrganizationServiceImpl",
                        "External API returned status: " + response.getStatusCode(),
                        "getPayrollGraphs",
                        "API_ERROR",
                        response.getBody() != null ? response.getBody().toString() : "External API returned status: " + response.getStatusCode());
            }

            return response;

        } catch (ServiceLevelException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Failed to get payroll graphs: " + e.getMessage(),
                    "getPayrollGraphs",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPayrollInsights(Long orgId, String month, Integer year) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(month) || ObjectUtils.isEmpty(year)) {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Organization ID, Month and Year are required",
                    "getPayrollInsights",
                    "VALIDATION_ERROR",
                    "Please provide valid Organization ID, Month and Year"
            );
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getPayrollInsightsUrl())
                    .queryParam("orgId", orgId)
                    .queryParam("month", month)
                    .queryParam("year", year);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
            if (!response.getStatusCode().is2xxSuccessful() || ObjectUtils.isEmpty(response.getBody())) {
                throw new ServiceLevelException(
                        "OrganizationServiceImpl",
                        "External API returned status: " + response.getStatusCode(),
                        "getPayrollInsights",
                        "API_ERROR",
                        response.getBody() != null ? response.getBody().toString() : "External API returned status: " + response.getStatusCode());
            }
            return response;
        } catch (ServiceLevelException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Failed to get payroll insights: " + e.getMessage(),
                    "getPayrollInsights",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage());

        }
    }

    @Override
    public ResponseEntity<?> createHrRequest(String requestBody, String token) {
        if (ObjectUtils.isEmpty(requestBody)) {
            throw new IllegalArgumentException("Request body cannot be null or empty");
        }
        ResponseEntity<?> response;
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getCreateHrRequestUrl());
//            Map<String, String> headers = commonUtils.buildJsonHeaders(token);
            // use objectMapper to convert string to json object
            Map<String, String> headers = new ConcurrentHashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            Object jsonBody = objectMapper.readValue(requestBody, Object.class);
            response = restService.iamRestCall(builder.toUriString(), jsonBody, headers, HttpMethod.POST, null);
            if (!response.getStatusCode().is2xxSuccessful() || ObjectUtils.isEmpty(response.getBody())) {
                throw new ServiceLevelException(
                        "OrganizationServiceImpl",
                        "Failed to create HR request: External API returned status: " + response.getStatusCode(),
                        "createHrRequest",
                        "API_ERROR",
                        response.getBody() != null ? response.getBody().toString() : "External API returned status: " + response.getStatusCode());
            }
        } catch (RuntimeException | JsonProcessingException e) {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Failed to create HR request: " + e.getMessage(),
                    "createHrRequest",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage());
        }

        return response;
    }

    @Override
    public ResponseEntity<?> getManyHrRequests(Long orgId, String requestType, String status, Integer page, Integer offset, String token) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getManyHrRequestsUrl())
                    .queryParam("orgId", orgId)
                    .queryParam("requestType", requestType)
                    .queryParam("status", status)
                    .queryParam("page", page)
                    .queryParam("offset", offset);
//            Map<String, String> headers = commonUtils.buildJsonHeaders(token);
            return restService.iamRestCall(builder.toUriString(), null, Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), HttpMethod.GET, null);
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Failed to get HR requests: " + e.getMessage(),
                    "getManyHrRequests",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage());
        }
    }

    @Override
    public ResponseEntity<?> takeActionOnHrRequest(Long requestId, String action, String resolutionRemarks, String token) {
        if (ObjectUtils.isEmpty(requestId) || ObjectUtils.isEmpty(action)) {
            throw new IllegalArgumentException("Request ID and action are required");
        }
        try {
            String userId = keycloakTokenUtil.extractUserId(token);
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getTakeActionOnHrRequestUrl())
                    .queryParam("requestId", requestId)
                    .queryParam("action", action)
                    .queryParam("resolutionRemarks", resolutionRemarks)
                    .queryParam("userId", Long.valueOf(userId));
//            Map<String, String> headers = commonUtils.buildJsonHeaders(token);
            return restService.iamRestCall(builder.toUriString(), null, Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), HttpMethod.POST, null);
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Failed to take action on HR request: " + e.getMessage(),
                    "takeActionOnHrRequest",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage());
        }
    }

    @Override
    public ResponseEntity<?> getClosedHrRequests(Long orgId, Integer page, Integer offset, String token) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new IllegalArgumentException("Organization ID is required");
        }
        ResponseEntity<?> response;
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getClosedHrRequestsUrl())
                    .queryParam("orgId", orgId)
                    .queryParam("page", page)
                    .queryParam("offset", offset);
//            Map<String, String> headers = commonUtils.buildJsonHeaders(token);
            response = restService.iamRestCall(builder.toUriString(), null, Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), HttpMethod.GET, null);
            if (!response.getStatusCode().is2xxSuccessful() || ObjectUtils.isEmpty(response.getBody())) {
                throw new ServiceLevelException(
                        "OrganizationServiceImpl",
                        "Failed to get closed HR requests: External API returned status: " + response.getStatusCode(),
                        "getClosedHrRequests",
                        "API_ERROR",
                        response.getBody() != null ? response.getBody().toString() : "External API returned status: " + response.getStatusCode());
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Failed to get closed HR requests: " + e.getMessage(),
                    "getClosedHrRequests",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage());
        }
        return response;
    }

    @Override
    public ResponseEntity<?> getHrRequestInsights(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new IllegalArgumentException("Organization ID is required");
        }
        ResponseEntity<?> response;
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getHrRequestInsightsUrl())
                    .queryParam("orgId", orgId);
//            Map<String, String> headers = commonUtils.buildJsonHeaders(token);
            response = restService.iamRestCall(builder.toUriString(), null, Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), HttpMethod.GET, null);
            if (!response.getStatusCode().is2xxSuccessful() || ObjectUtils.isEmpty(response.getBody())) {
                throw new ServiceLevelException(
                        "OrganizationServiceImpl",
                        "Failed to get HR request insights: External API returned status: " + response.getStatusCode(),
                        "getHrRequestInsights",
                        "API_ERROR",
                        response.getBody() != null ? response.getBody().toString() : "External API returned status: " + response.getStatusCode());
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Failed to get HR request insights: " + e.getMessage(),
                    "getHrRequestInsights",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage());
        }
        return response;
    }

    @Override
    public ResponseEntity<?> getTimeManagementQuickUpdate(Long empId) {
        if (ObjectUtils.isEmpty(empId)) {
            throw new IllegalArgumentException("Employee ID is required");
        }
        ResponseEntity<?> response;
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getTimeManagementQuickUpdateUrl())
                    .queryParam("empId", empId);
            response = restService.iamRestCall(builder.toUriString(), null, Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), HttpMethod.GET, null);
            if (!response.getStatusCode().is2xxSuccessful() || ObjectUtils.isEmpty(response.getBody())) {
                throw new ServiceLevelException(
                        "OrganizationServiceImpl",
                        "Failed to get time management quick update: External API returned status: " + response.getStatusCode(),
                        "getTimeManagementQuickUpdate",
                        "API_ERROR",
                        response.getBody() != null ? response.getBody().toString() : "External API returned status: " + response.getStatusCode());
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Failed to get time management quick update: " + e.getMessage(),
                    "getTimeManagementQuickUpdate",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage());
        }

        return response;
    }

    @Override
    public ResponseEntity<?> getHeroAnalytics(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new IllegalArgumentException("Organization ID is required");
        }
        ResponseEntity<?> response;
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getHeroAnalyticsUrl())
                    .queryParam("orgId", orgId);
            response = restService.iamRestCall(builder.toUriString(), null, Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), HttpMethod.GET, null);
            if (!response.getStatusCode().is2xxSuccessful() || ObjectUtils.isEmpty(response.getBody())) {
                throw new ServiceLevelException(
                        "OrganizationServiceImpl",
                        "Failed to get hero analytics: External API returned status: " + response.getStatusCode(),
                        "getHeroAnalytics",
                        "API_ERROR",
                        response.getBody() != null ? response.getBody().toString() : "External API returned status: " + response.getStatusCode());
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "OrganizationServiceImpl",
                    "Failed to get hero analytics: " + e.getMessage(),
                    "getHeroAnalytics",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage());
        }
        return response;
    }
}

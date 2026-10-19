package com.nexus.iam.service.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import com.nexus.iam.dto.LoginResponse;
import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.Gender;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.entities.Role;
import com.nexus.iam.entities.User;
import com.nexus.iam.repository.DepartmentRepository;
import com.nexus.iam.repository.OrganizationRepository;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.service.CoreLogisticsService;
import com.nexus.iam.service.KeycloakAuthenticationService;
import com.nexus.iam.utils.CommonConstants;
import com.nexus.iam.utils.CommonUtils;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;

import lombok.RequiredArgsConstructor;

/**
 * Core Logistics Service Implementation
 * <p>
 * Handles logistics-specific Core module operations through IAM gateway.
 * All HTTP calls to Core module are handled here using RestService.
 */
@Service
@RequiredArgsConstructor
public class CoreLogisticsServiceImpl implements CoreLogisticsService {

    private final RestService restService;
    private final WebConstants webConstants;
    private final CommonUtils commonUtils;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakAuthenticationService keycloakAuthenticationService;

    // Fleet assets
    @Override
    public ResponseEntity<?> createAsset(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsFleetAssetCreateUrl(), dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getAsset(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsFleetAssetGetUrl() + "/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllAssets(String auth, String org, Pageable p, String status, String assetType, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsFleetAssetAllUrl(), p,
                "status", status, "assetType", assetType, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> updateAsset(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsFleetAssetGetUrl() + "/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> transitionAssetStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsFleetAssetGetUrl() + "/" + id + "/status?newStatus=" + newStatus, params, auth, org);
    }

    @Override
    public ResponseEntity<?> deleteAsset(Long id, String auth, String org) {
        return callDelete(webConstants.getCoreLogisticsFleetAssetGetUrl() + "/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAssetSummary(String auth, String org) {
        return callGet(webConstants.getCoreLogisticsFleetAssetSummaryUrl(), auth, org);
    }

    @Override
    public ResponseEntity<?> getAssetShipments(Long assetId, String auth, String org, Pageable p, String status, String from, String to) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsFleetAssetGetUrl() + "/" + assetId + "/shipments", p,
                "status", status, "from", from, "to", to), auth, org);
    }

    @Override
    public ResponseEntity<?> getAssetDrivers(Long assetId, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsFleetAssetGetUrl() + "/" + assetId + "/drivers", auth, org);
    }

    @Override
    public ResponseEntity<?> getAssetCurrentShipment(Long assetId, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsFleetAssetGetUrl() + "/" + assetId + "/current-shipment", auth, org);
    }

    // Drivers
    @Override
    public ResponseEntity<?> createDriver(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsDriverCreateUrl(), dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getDriver(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsDriverGetUrl() + "/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllDrivers(String auth, String org, Pageable p, String status, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsDriverAllUrl(), p,
                "status", status, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> updateDriver(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsDriverGetUrl() + "/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> transitionDriverStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsDriverGetUrl() + "/" + id + "/status?newStatus=" + newStatus, params, auth, org);
    }

    @Override
    public ResponseEntity<?> deleteDriver(Long id, String auth, String org) {
        return callDelete(webConstants.getCoreLogisticsDriverGetUrl() + "/" + id, auth, org);
    }

    // Driver employee-onboarding: IAM user (DRIVER role) → HR record → linked core driver

    /** Result of creating an employee's IAM user + HR record during onboarding. */
    private record CreatedEmployee(User user, String password, Long hrId, Map<String, Object> urls) {
    }

    @Override
    public ResponseEntity<?> onboardDriver(Map<String, Object> body, String auth, String org) {
        try {
            String email = objStr(body.get("email"));
            if (email == null || email.isBlank())
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Login email is required to onboard a driver as an employee"));
            if (Boolean.TRUE.equals(userRepository.existsByEmail(email)))
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "A user with this email already exists: " + email));

            CreatedEmployee created = createDriverEmployee(body, org);

            Map<String, Object> driverDto = driverDtoFrom(body);
            driverDto.put("iamUserId", created.user().getId());
            if (created.hrId() != null)
                driverDto.put("hrEmployeeId", String.valueOf(created.hrId()));

            ResponseEntity<?> coreResponse = callPost(webConstants.getCoreLogisticsDriverCreateUrl(), driverDto,
                    auth, org);
            if (!coreResponse.getStatusCode().is2xxSuccessful()) {
                // IAM user + HR record are already in place; surface everything for recovery
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("userId", created.user().getId());
                out.put("email", created.user().getEmail());
                out.put("password", created.password());
                if (created.hrId() != null) out.put("hrId", created.hrId());
                out.put("error", "IAM user and HR record were created but the core driver record failed: "
                        + coreResponse.getBody());
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(out);
            }
            Map<String, Object> out = respToMap(coreResponse);
            if (out == null) out = new LinkedHashMap<>();
            out.put("password", created.password());
            if (created.hrId() != null) out.put("hrId", created.hrId());
            out.putAll(created.urls());
            out.put("message", "Driver onboarded as an employee. Share the one-time password with them.");
            return ResponseEntity.status(HttpStatus.CREATED).body(out);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Driver onboarding failed: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<?> getDriverOnboardDetails(Long id, String auth, String org) {
        try {
            Map<String, Object> existing = respToMap(
                    callGet(webConstants.getCoreLogisticsDriverGetUrl() + "/" + id, auth, org));
            if (existing == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Driver not found"));
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("driver", existing);

            Long iamUserId = existing.get("iamUserId") instanceof Number n ? n.longValue() : null;
            if (iamUserId != null) {
                User user = userRepository.findById(iamUserId).orElse(null);
                if (user != null) {
                    Map<String, Object> iamUser = new LinkedHashMap<>();
                    iamUser.put("userId", user.getId());
                    iamUser.put("email", user.getEmail());
                    iamUser.put("name", user.getName());
                    iamUser.put("phone", user.getPhone());
                    iamUser.put("address", user.getAddress());
                    iamUser.put("personalEmail", user.getPersonalEmail());
                    iamUser.put("gender", user.getGender() != null ? user.getGender().name() : null);
                    iamUser.put("age", user.getAge());
                    iamUser.put("dateOfBirth", user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null);
                    out.put("iamUser", iamUser);
                }
            }

            // HR record (department + bank records) — optional, ignore failures
            if (iamUserId != null) {
                try {
                    ResponseEntity<String> hrResponse = restService.iamRestCall(
                            UriComponentsBuilder.fromUriString(webConstants.getEmployeeDetailsUrl())
                                    .queryParam("empId", iamUserId).toUriString(),
                            null, genericServiceHeaders(), HttpMethod.GET, null);
                    if (hrResponse.getStatusCode().is2xxSuccessful()) {
                        Map<String, Object> hrBody = restService.parseJsonResponse(hrResponse.getBody());
                        if (hrBody != null) {
                            if (hrBody.get("department") != null)
                                out.put("department", String.valueOf(hrBody.get("department")));
                            if (hrBody.get("compensation") instanceof Map<?, ?> comp) {
                                Map<String, Object> compMap = new LinkedHashMap<>();
                                comp.forEach((k, v) -> compMap.put(String.valueOf(k), v));
                                if (compMap.get("bankRecords") != null)
                                    out.put("bankRecords", compMap.get("bankRecords"));
                            }
                        }
                    }
                } catch (Exception ex) {
                    // HR record missing or unreachable — return what we have
                }
            }
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to load driver employee details: " + e.getMessage()));
        }
    }

    /** Service-account headers for internal service-to-service calls. */
    private Map<String, String> genericServiceHeaders() {
        Map<String, String> headers = new HashMap<>();
        LoginResponse loginResponse = keycloakAuthenticationService
                .login(webConstants.getGenericUserId(), webConstants.getGenericPassword()).getBody();
        headers.put(CommonConstants.AUTHORIZATION, "Bearer " + loginResponse.getAccessToken());
        headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.APPLICATION_JSON);
        return headers;
    }

    @Override
    public ResponseEntity<?> syncDriverOnboard(Long id, Map<String, Object> body, String auth, String org) {
        try {
            Map<String, Object> existing = respToMap(
                    callGet(webConstants.getCoreLogisticsDriverGetUrl() + "/" + id, auth, org));
            if (existing == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Driver not found"));
            Object existingIam = existing.get("iamUserId");
            Map<String, Object> driverDto = driverDtoFrom(body);
            Map<String, Object> out = new LinkedHashMap<>();

            Long linkedUserId = existingIam instanceof Number n ? n.longValue()
                    : existingIam != null && !String.valueOf(existingIam).isBlank()
                            ? Long.valueOf(String.valueOf(existingIam)) : null;

            if (linkedUserId != null) {
                // Already linked → sync profile changes into the IAM user
                User user = userRepository.findById(linkedUserId).orElse(null);
                if (user == null)
                    return ResponseEntity.badRequest().body(Map.of("error",
                            "Driver is linked to IAM user " + linkedUserId + " which no longer exists"));
                String newEmail = objStr(body.get("email"));
                if (newEmail != null && !newEmail.isBlank() && !newEmail.equalsIgnoreCase(user.getEmail())
                        && Boolean.TRUE.equals(userRepository.existsByEmail(newEmail)))
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "A user with this email already exists: " + newEmail));
                applyProfile(user, body);
                userRepository.save(user);
                driverDto.put("iamUserId", user.getId());
                // keep the logistics defaults present even for existing orgs
                Role driverRole = roleRepository.findByName("DRIVER").orElseGet(() -> {
                    Role r = new Role();
                    r.setName("DRIVER");
                    return roleRepository.save(r);
                });
                ensureDriverDepartment(user.getOrganization() != null
                        ? user.getOrganization()
                        : organizationRepository.findById(Long.valueOf(org)).orElse(null), driverRole);
                out.put("message", "Driver details synced to IAM user and core record");
            } else {
                // Not linked → create the missing IAM user + HR record from the provided details
                String email = objStr(body.get("email"));
                if (email == null || email.isBlank())
                    return ResponseEntity.badRequest().body(Map.of("error",
                            "Email is required to create this driver's IAM user and HR record"));
                if (Boolean.TRUE.equals(userRepository.existsByEmail(email)))
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "A user with this email already exists: " + email));
                CreatedEmployee created = createDriverEmployee(body, org);
                driverDto.put("iamUserId", created.user().getId());
                if (created.hrId() != null)
                    driverDto.put("hrEmployeeId", String.valueOf(created.hrId()));
                out.put("password", created.password());
                if (created.hrId() != null) out.put("hrId", created.hrId());
                out.putAll(created.urls());
                out.put("message", "Driver linked to a new IAM user and HR record. Share the one-time password.");
            }

            ResponseEntity<?> coreResponse = callPut(
                    webConstants.getCoreLogisticsDriverGetUrl() + "/" + id + "/update", driverDto, auth, org);
            if (!coreResponse.getStatusCode().is2xxSuccessful())
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "IAM/HR update succeeded but the core driver update failed: "
                                + coreResponse.getBody()));
            Map<String, Object> resp = respToMap(coreResponse);
            if (resp != null) out.putAll(resp);
            if (out.get("password") == null) out.remove("password");
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Driver update failed: " + e.getMessage()));
        }
    }

    /**
     * Shared half of onboarding: creates the IAM user with the DRIVER role and
     * a generated one-time password, then POSTs the HR employee-init payload
     * (compensation + bank records) exactly like the HR app's add-employee
     * flow does through IAM.
     */
    @SuppressWarnings("unchecked")
    private CreatedEmployee createDriverEmployee(Map<String, Object> body, String org) {
        User user = new User();
        user.setName(objStr(body.get("fullName")));
        user.setEmail(objStr(body.get("email")));
        user.setPhone(objStr(body.get("phone")));
        user.setAddress(objStr(body.get("address")));
        user.setPersonalEmail(objStr(body.get("personalEmail")));
        user.setGender(parseGender(objStr(body.get("gender"))));
        String age = objStr(body.get("age"));
        if (age != null && !age.isBlank()) user.setAge(Integer.valueOf(age));
        user.setDateOfBirth(parseIsoDate(objStr(body.get("dateOfBirth"))));
        user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        user.setOrganization(organizationRepository.findById(Long.valueOf(org))
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + org)));

        // DRIVER role + Operations department, created on the fly for orgs
        // that predate the logistics onboarding defaults
        Role driverRole = roleRepository.findByName("DRIVER").orElseGet(() -> {
            Role r = new Role();
            r.setName("DRIVER");
            return roleRepository.save(r);
        });
        user.getRoles().add(driverRole);
        Department driverDept = ensureDriverDepartment(user.getOrganization(), driverRole);

        String password = generatedPassword();
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        userRepository.save(user);
        driverDept.addMember(user);
        departmentRepository.save(driverDept);
        userRepository.save(user);

        // HR employee record (same /hr/employee/init contract UserServiceImpl uses)
        Map<String, Object> compensation = new LinkedHashMap<>();
        Object comp = body.get("compensation");
        if (comp instanceof Map<?, ?> c)
            c.forEach((k, v) -> compensation.put(String.valueOf(k), v));
        compensation.putIfAbsent("bonuses", new ArrayList<>());
        compensation.putIfAbsent("deductions", new ArrayList<>());
        Object banks = body.get("bankRecords");
        compensation.putIfAbsent("bankRecords", banks != null ? banks : new ArrayList<>());

        Map<String, Object> hrPayload = new LinkedHashMap<>();
        hrPayload.put("employeeId", user.getId());
        hrPayload.put("fullName", user.getName());
        hrPayload.put("email", user.getEmail());
        hrPayload.put("orgId", user.getOrganization().getId());
        hrPayload.put("orgName", user.getOrganization().getOrgName());
        hrPayload.put("department", driverDept.getDepartmentName() != null
                ? driverDept.getDepartmentName() : "Operations");
        hrPayload.put("title", orDefault(objStr(body.get("title")), "Driver"));
        hrPayload.put("remarks", objStr(body.get("notes")));
        hrPayload.put("timestamp", new Timestamp(System.currentTimeMillis()));
        hrPayload.put("personalEmail", user.getPersonalEmail());
        hrPayload.put("compensation", compensation);
        hrPayload.put("employeeName", user.getName());
        hrPayload.put("employeeEmail", user.getEmail());
        hrPayload.put("employeePersonalEmail", user.getPersonalEmail());
        hrPayload.put("gender", user.getGender() != null ? user.getGender().name() : "MALE");

        Map<String, String> headers = genericServiceHeaders();

        ResponseEntity<String> hrResponse = restService.iamRestCall(
                UriComponentsBuilder.fromUriString(webConstants.getHrInitUrl()).toUriString(),
                hrPayload, headers, HttpMethod.POST, user.getId());

        Long hrId = null;
        Map<String, Object> urls = new LinkedHashMap<>();
        if (hrResponse.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> respBody = restService.parseJsonResponse(hrResponse.getBody());
            if (respBody == null)
                throw new IllegalStateException("HR initialization returned an empty body");
            Object hr = respBody.get("hrId");
            if (hr instanceof Number n) hrId = n.longValue();
            for (String k : List.of("joiningLetterUrl", "letterOfIntentUrl", "compensationCardUrl"))
                if (respBody.get(k) != null) urls.put(k, String.valueOf(respBody.get(k)));
        } else {
            throw new IllegalStateException("HR initialization failed with status " + hrResponse.getStatusCode()
                    + ": " + hrResponse.getBody());
        }
        return new CreatedEmployee(user, password, hrId, urls);
    }

    /** DRIVER role + Operations department of the org, created if missing. */
    private Department ensureDriverDepartment(Organization organization, Role driverRole) {
        return departmentRepository.findByOrgId(organization.getId()).stream()
                .filter(d -> "Operations".equalsIgnoreCase(d.getDepartmentName()))
                .findFirst()
                .orElseGet(() -> {
                    Department dept = new Department();
                    dept.setDepartmentName("Operations");
                    dept.setOrganization(organization);
                    dept.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                    dept.getRoles().add(driverRole);
                    return departmentRepository.save(dept);
                });
    }

    /** Driver-record fields only (strips employee/IAM-only fields). */
    @SuppressWarnings("unchecked")
    private Map<String, Object> driverDtoFrom(Map<String, Object> body) {
        Map<String, Object> dto = new LinkedHashMap<>();
        for (String k : List.of("driverCode", "fullName", "phone", "email", "licenseNumber", "licenseClass",
                "licenseExpiry", "status", "safetyScore", "onTimeRate", "totalTrips", "notes", "hrEmployeeId")) {
            Object v = body.get(k);
            if (v != null && !String.valueOf(v).isBlank())
                dto.put(k, v);
        }
        return dto;
    }

    /** Null-safe profile application for a linked (existing) IAM user. */
    private void applyProfile(User user, Map<String, Object> body) {
        String v = objStr(body.get("fullName"));
        if (v != null && !v.isBlank()) user.setName(v);
        if ((v = objStr(body.get("phone"))) != null) user.setPhone(v);
        if ((v = objStr(body.get("address"))) != null) user.setAddress(v);
        if ((v = objStr(body.get("personalEmail"))) != null) user.setPersonalEmail(v);
        Gender g = parseGender(objStr(body.get("gender")));
        if (g != null) user.setGender(g);
        String age = objStr(body.get("age"));
        if (age != null && !age.isBlank()) user.setAge(Integer.valueOf(age));
        java.sql.Date dob = parseIsoDate(objStr(body.get("dateOfBirth")));
        if (dob != null) user.setDateOfBirth(dob);
    }

    private Gender parseGender(String g) {
        if (g == null || g.isBlank()) return null;
        return switch (g.trim().toUpperCase()) {
            case "M", "MALE" -> Gender.MALE;
            case "F", "FEMALE" -> Gender.FEMALE;
            case "O", "OTHER" -> Gender.OTHER;
            default -> null;
        };
    }

    private java.sql.Date parseIsoDate(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return java.sql.Date.valueOf(v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String objStr(Object v) {
        return v != null ? String.valueOf(v) : null;
    }

    private String orDefault(String v, String fallback) {
        return v != null && !v.isBlank() ? v : fallback;
    }

    private Map<String, Object> respToMap(ResponseEntity<?> resp) {
        if (resp == null || resp.getBody() == null) return null;
        if (resp.getBody() instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        return restService.parseJsonResponse(String.valueOf(resp.getBody()));
    }

    /** Random 12-char password with at least one char of each class. */
    private String generatedPassword() {
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String specialChars = "!@#$%^&*";
        String allChars = uppercase + lowercase + digits + specialChars;
        java.util.Random random = new java.util.Random();
        StringBuilder password = new StringBuilder();
        password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));
        for (int i = 0; i < 8; i++)
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        return password.toString();
    }

    // Maintenance
    @Override
    public ResponseEntity<?> createMaintenance(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsMaintenanceCreateUrl(), dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getMaintenance(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsMaintenanceGetUrl() + "/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllMaintenance(String auth, String org, Pageable p, Long assetId, String status, Boolean isBreakdown, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsMaintenanceAllUrl(), p,
                "assetId", assetId, "status", status, "isBreakdown", isBreakdown, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> transitionMaintenanceStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsMaintenanceGetUrl() + "/" + id + "/status?newStatus=" + newStatus, params, auth, org);
    }

    @Override
    public ResponseEntity<?> updateMaintenance(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsMaintenanceGetUrl() + "/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> deleteMaintenance(Long id, String auth, String org) {
        return callDelete(webConstants.getCoreLogisticsMaintenanceGetUrl() + "/" + id, auth, org);
    }

    // Load board + quotes + rates
    @Override
    public ResponseEntity<?> getLoadBoard(String auth, String org, Pageable p, String mode, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsLoadBoardUrl(), p,
                "mode", mode, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> getPickedUpLoads(String auth, String org, Pageable p) {
        return callGet(buildPaginatedUrl(webConstants.getCoreServiceUrl() + "/core/logistics/quoting/picked-up", p), auth, org);
    }

    @Override
    public ResponseEntity<?> getDeliveredLoads(String auth, String org, Pageable p) {
        return callGet(buildPaginatedUrl(webConstants.getCoreServiceUrl() + "/core/logistics/quoting/delivered", p), auth, org);
    }

    @Override
    public ResponseEntity<?> getMyShipmentBoard(String auth, String org, Pageable p, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreServiceUrl() + "/core/logistics/quoting/my-shipments", p,
                "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> createQuote(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsQuoteCreateUrl(), dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getQuote(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsQuoteGetUrl() + "/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllQuotes(String auth, String org, Pageable p, Long shipmentId, String status, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsQuoteAllUrl(), p,
                "shipmentId", shipmentId, "status", status, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> updateQuote(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsQuoteGetUrl() + "/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> transitionQuoteStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsQuoteGetUrl() + "/" + id + "/status?newStatus=" + newStatus, params, auth, org);
    }

    @Override
    public ResponseEntity<?> deleteQuote(Long id, String auth, String org) {
        return callDelete(webConstants.getCoreLogisticsQuoteGetUrl() + "/" + id, auth, org);
    }

    // Execution
    @Override
    public ResponseEntity<?> assignBooking(Long shipmentId, Map<String, Object> body, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsExecutionBaseUrl() + "/shipments/" + shipmentId + "/assign", body, auth, org);
    }

    @Override
    public ResponseEntity<?> transitionShipmentStatus(Long shipmentId, String newStatus, Map<String, Object> params, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsExecutionBaseUrl() + "/shipments/" + shipmentId + "/status?newStatus=" + newStatus, params, auth, org);
    }

    @Override
    public ResponseEntity<?> getShipmentPosition(Long shipmentId, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsExecutionBaseUrl() + "/shipments/" + shipmentId + "/position", auth, org);
    }

    @Override
    public ResponseEntity<?> updateShipmentRoute(Long shipmentId, Map<String, Object> body, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsExecutionBaseUrl() + "/shipments/" + shipmentId + "/route", body, auth, org);
    }
    @Override
    public ResponseEntity<?> getShipmentEta(Long shipmentId, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsExecutionBaseUrl() + "/shipments/" + shipmentId + "/eta", auth, org);
    }

    @Override
    public ResponseEntity<?> capturePod(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsExecutionBaseUrl() + "/pod/capture", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> updatePod(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsExecutionBaseUrl() + "/pod/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getPodByShipment(Long shipmentId, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsExecutionBaseUrl() + "/pod/shipment/" + shipmentId, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllPods(String auth, String org, Pageable p, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsExecutionBaseUrl() + "/pod/all", p,
                "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> reportIncident(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsExecutionBaseUrl() + "/incidents/report", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> updateIncident(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsExecutionBaseUrl() + "/incidents/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getIncident(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsExecutionBaseUrl() + "/incidents/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllIncidents(String auth, String org, Pageable p, Long shipmentId, String incidentType, String status, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsExecutionBaseUrl() + "/incidents/all", p,
                "shipmentId", shipmentId, "incidentType", incidentType, "status", status, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> transitionIncidentStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsExecutionBaseUrl() + "/incidents/" + id + "/status?newStatus=" + newStatus, params, auth, org);
    }

    // Operations
    @Override
    public ResponseEntity<?> createGroup(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsOperationsBaseUrl() + "/consolidation/create", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getGroup(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsOperationsBaseUrl() + "/consolidation/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllGroups(String auth, String org, Pageable p, String status, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsOperationsBaseUrl() + "/consolidation/all", p,
                "status", status, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> updateGroup(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsOperationsBaseUrl() + "/consolidation/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> transitionGroupStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsOperationsBaseUrl() + "/consolidation/" + id + "/status?newStatus=" + newStatus, params, auth, org);
    }

    @Override
    public ResponseEntity<?> addShipmentsToGroup(Long id, Map<String, Object> body, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsOperationsBaseUrl() + "/consolidation/" + id + "/shipments", body, auth, org);
    }

    @Override
    public ResponseEntity<?> deleteGroup(Long id, String auth, String org) {
        return callDelete(webConstants.getCoreLogisticsOperationsBaseUrl() + "/consolidation/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> createCapacity(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsOperationsBaseUrl() + "/capacity/create", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getCapacity(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsOperationsBaseUrl() + "/capacity/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllCapacities(String auth, String org, Pageable p, String equipmentType, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsOperationsBaseUrl() + "/capacity/all", p,
                "equipmentType", equipmentType, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> updateCapacity(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsOperationsBaseUrl() + "/capacity/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> deleteCapacity(Long id, String auth, String org) {
        return callDelete(webConstants.getCoreLogisticsOperationsBaseUrl() + "/capacity/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> extendCapacityForPartnership(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsOperationsBaseUrl() + "/capacity/" + id + "/extend-for-partnership", dto, auth, org);
    }

    private String partnershipBaseUrl() {
        return webConstants.getCoreServiceUrl() + "/core/logistics/partnerships";
    }

    @Override
    public ResponseEntity<?> createPartnershipQuotation(Map<String, Object> dto, String auth, String org) {
        return callPost(partnershipBaseUrl() + "/quotations", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getPartnershipQuotations(String auth, String org, Pageable p, String status) {
        return callGet(buildPaginatedUrlWithFilters(partnershipBaseUrl() + "/quotations", p, "status", status), auth, org);
    }

    @Override
    public ResponseEntity<?> getPartnershipQuotation(Long id, String auth, String org) {
        return callGet(partnershipBaseUrl() + "/quotations/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getPartnershipRoutes(Long id, String auth, String org, Pageable p) {
        return callGet(buildPaginatedUrl(partnershipBaseUrl() + "/" + id + "/routes", p), auth, org);
    }

    @Override
    public ResponseEntity<?> activatePartnership(Long id, String auth, String org) {
        return callPost(partnershipBaseUrl() + "/" + id + "/activate", null, auth, org);
    }

    @Override
    public ResponseEntity<?> terminatePartnership(Long id, Map<String, Object> dto, String auth, String org) {
        return callPost(partnershipBaseUrl() + "/" + id + "/terminate", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getLogisticsDashboard(String auth, String org) {
        return callGet(webConstants.getCoreLogisticsOperationsBaseUrl() + "/analytics/dashboard", auth, org);
    }

    // Partnership Invitations (same core URLs as retailer service)
    @Override
    public ResponseEntity<?> createPartnershipInvitation(Map<String, Object> invitationDto, String auth, String org) {
        return callPost(webConstants.getCorePartnershipInvitationCreateUrl(), invitationDto, auth, org);
    }

    @Override
    public ResponseEntity<?> respondToPartnershipInvitation(Long id, Map<String, Object> responseDto, String auth, String org) {
        return callPut(webConstants.getCorePartnershipInvitationRespondUrl() + "/" + id + "/respond", responseDto, auth, org);
    }

    @Override
    public ResponseEntity<?> getPartnershipInvitation(Long id, String auth, String org) {
        return callGet(webConstants.getCorePartnershipInvitationGetUrl() + "/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getSentPartnershipInvitations(String auth, String org, Pageable p) {
        return callGet(buildPaginatedUrl(webConstants.getCorePartnershipInvitationSentUrl(), p), auth, org);
    }

    @Override
    public ResponseEntity<?> getReceivedPartnershipInvitations(String auth, String org, Pageable p) {
        return callGet(buildPaginatedUrl(webConstants.getCorePartnershipInvitationReceivedUrl(), p), auth, org);
    }

    @Override
    public ResponseEntity<?> getPendingPartnershipInvitations(String auth, String org, Pageable p) {
        return callGet(buildPaginatedUrl(webConstants.getCorePartnershipInvitationPendingUrl(), p), auth, org);
    }

    @Override
    public ResponseEntity<?> withdrawPartnershipInvitation(Long id, String auth, String org) {
        return callPut(webConstants.getCorePartnershipInvitationWithdrawUrl() + "/" + id + "/withdraw", null, auth, org);
    }

    // Logistics shipment list (Core GET /core/shipments/by-logistics-org)
    @Override
    public ResponseEntity<?> getMyShipments(String auth, String org, Pageable p) {
        return callGet(buildPaginatedUrl(webConstants.getCoreServiceUrl() + "/core/shipments/by-logistics-org", p), auth, org);
    }

    @Override
    public ResponseEntity<?> getAllPartnerships(String auth, String org, Pageable p) {
        return callGet(buildPaginatedUrl(webConstants.getCorePartnershipMineUrl(), p), auth, org);
    }

    @Override
    public ResponseEntity<?> updatePartnership(Long id, Map<String, Object> partnershipDto, String auth, String org) {
        return callPut(webConstants.getCorePartnershipGetUrl() + "/" + id + "/update", partnershipDto, auth, org);
    }

    @Override
    public ResponseEntity<?> updatePartnershipStatus(Long id, String status, String auth, String org) {
        return callPost(webConstants.getCorePartnershipUpdateStatusUrl() + "/" + id + "/status", Map.of("status", status), auth, org);
    }

    // Finance analytics + settlement statements (suite finances pages)
    private String financeBaseUrl() {
        return webConstants.getCoreServiceUrl() + "/core/finance";
    }

    @Override
    public ResponseEntity<?> getLogisticsFinanceSummary(String period, String auth, String org) {
        String url = financeBaseUrl() + "/logistics/summary";
        if (period != null)
            url += "?period=" + period;
        return callGet(url, auth, org);
    }

    @Override
    public ResponseEntity<?> getLogisticsFinanceTransactions(String kind, String status, String search,
            String from, String to, String auth, String org, Pageable p) {
        return callGet(buildPaginatedUrlWithFilters(financeBaseUrl() + "/logistics/transactions", p,
                "kind", kind, "status", status, "search", search, "from", from, "to", to), auth, org);
    }

    @Override
    public ResponseEntity<?> getBillingStatements(Long partnershipId, String auth, String org) {
        return callGet(financeBaseUrl() + "/billing/statements?partnershipId=" + partnershipId, auth, org);
    }

    @Override
    public ResponseEntity<?> getBillingOpenTrips(Long partnershipId, String auth, String org) {
        return callGet(financeBaseUrl() + "/billing/open-trips?partnershipId=" + partnershipId, auth, org);
    }

    private ResponseEntity<?> callGet(String url, String authToken, String orgId) {
        Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
        headers.put("X-Organization-ID", orgId);
        return restService.iamRestCall(url, null, headers, HttpMethod.GET, null);
    }

    private ResponseEntity<?> callPost(String url, Object body, String authToken, String orgId) {
        Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
        headers.put("X-Organization-ID", orgId);
        return restService.iamRestCall(url, body, headers, HttpMethod.POST, null);
    }

    private ResponseEntity<?> callPut(String url, Object body, String authToken, String orgId) {
        Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
        headers.put("X-Organization-ID", orgId);
        return restService.iamRestCall(url, body, headers, HttpMethod.PUT, null);
    }

    private ResponseEntity<?> callDelete(String url, String authToken, String orgId) {
        Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
        headers.put("X-Organization-ID", orgId);
        return restService.iamRestCall(url, null, headers, HttpMethod.DELETE, null);
    }

    private String buildPaginatedUrl(String baseUrl, Pageable pageable) {
        if (pageable == null || pageable.isUnpaged())
            return baseUrl;
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("page", pageable.getPageNumber()).queryParam("size", pageable.getPageSize());
        if (pageable.getSort().isSorted())
            for (Sort.Order o : pageable.getSort())
                builder.queryParam("sort", o.getProperty() + "," + o.getDirection().name());
        return builder.toUriString();
    }

    private String buildPaginatedUrlWithFilters(String baseUrl, Pageable pageable, Object... filters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
        if (pageable != null && !pageable.isUnpaged()) {
            builder.queryParam("page", pageable.getPageNumber()).queryParam("size", pageable.getPageSize());
            if (pageable.getSort().isSorted())
                for (Sort.Order o : pageable.getSort())
                    builder.queryParam("sort", o.getProperty() + "," + o.getDirection().name());
        }
        for (int i = 0; i < filters.length; i += 2)
            if (i + 1 < filters.length && filters[i + 1] != null)
                builder.queryParam((String) filters[i], filters[i + 1]);
        return builder.toUriString();
    }
}

package com.nexus.iam.service.impl;

import com.nexus.iam.dto.LoginRequest;
import com.nexus.iam.dto.LoginResponse;
import com.nexus.iam.dto.RefreshTokenRequest;
import com.nexus.iam.dto.UserRegisterDto;
import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.entities.Role;
import com.nexus.iam.entities.User;
import com.nexus.iam.repository.DepartmentRepository;
import com.nexus.iam.repository.OrganizationRepository;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.security.JwtUtil;
import com.nexus.iam.service.AuthenticationService;
import com.nexus.iam.utils.CommonConstants;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final OrganizationRepository organizationRepository;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final WebConstants webConstants;
    private final ModelMapper modelMapper;
    private final RestService restService;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse authenticate(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserDetails)) {
            throw new IllegalArgumentException("Invalid authentication");
        }

        UserDetails userDetails = (UserDetails) principal;

        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        // Fetch fresh user data from database to get latest roles and organization
        // Using eager loading to ensure roles are fetched immediately
        User dbUser = userRepository.findByEmail(userDetails.getUsername()).orElse(null);

        // Determine role - prioritize database role over UserDetails role
        String role = "ROLE_USER";
        if (dbUser != null && dbUser.getRoles() != null && !dbUser.getRoles().isEmpty()) {
            role = dbUser.getRoles().stream()
                    .findFirst()
                    .map(r -> "ROLE_" + r.getName())
                    .orElse("ROLE_USER");
        } else if (userDetails.getAuthorities() != null && !userDetails.getAuthorities().isEmpty()) {
            role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(grantedAuthority -> grantedAuthority.getAuthority())
                    .orElse("ROLE_USER");
        }

        // Determine orgId
        Long orgId = null;
        if (dbUser != null && dbUser.getOrganization() != null) {
            orgId = dbUser.getOrganization().getId();
        }

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .email(userDetails.getUsername())
                .role(role)
                .userId(dbUser != null ? dbUser.getId() : null)
                .name(dbUser != null ? dbUser.getName() : "")
                .orgId(orgId)
                .build();
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();

        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String username = jwtUtil.extractUsernameFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String newAccessToken = jwtUtil.generateAccessToken(userDetails);

        // Fetch fresh user data from database to get latest roles and organization
        User dbUser = userRepository.findByEmail(username).orElse(null);

        // Determine role - prioritize database role over UserDetails role
        String role = "ROLE_USER";
        if (dbUser != null && dbUser.getRoles() != null && !dbUser.getRoles().isEmpty()) {
            role = dbUser.getRoles().stream()
                    .findFirst()
                    .map(r -> "ROLE_" + r.getName())
                    .orElse("ROLE_USER");
        } else if (userDetails != null && userDetails.getAuthorities() != null
                && !userDetails.getAuthorities().isEmpty()) {
            role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(grantedAuthority -> grantedAuthority.getAuthority())
                    .orElse("ROLE_USER");
        }

        // Determine orgId
        Long orgId = null;
        if (dbUser != null && dbUser.getOrganization() != null) {
            orgId = dbUser.getOrganization().getId();
        }

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .email(userDetails.getUsername())
                .role(role)
                .userId(dbUser != null ? dbUser.getId() : null)
                .name(dbUser != null ? dbUser.getName() : "")
                .orgId(orgId)
                .build();
    }

    @Override
    public LoginResponse registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        try {
            User savedUser = userRepository.save(user);

            // Load user details and generate tokens
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            String accessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(900)
                    .email(userDetails.getUsername())
                    .role(savedUser.getRoles().stream()
                            .findFirst()
                            .map(r -> "ROLE_" + r.getName())
                            .orElse("ROLE_USER"))
                    .name(savedUser.getName())
                    .orgId(savedUser.getOrganization().getId())
                    .userId(
                            userRepository.findByEmail(user.getUsername()).map(
                                    User::getId).orElse(null)

                    )
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to register user: " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public LoginResponse registerUser(UserRegisterDto userRegisterDto, MultipartFile profilePhoto) {
        if (ObjectUtils.isEmpty(userRegisterDto)) {
            throw new IllegalArgumentException("User registration data cannot be null or empty");
        }
        if (ObjectUtils.isEmpty(userRegisterDto.getName()) ||
                ObjectUtils.isEmpty(userRegisterDto.getEmail()) ||
                ObjectUtils.isEmpty(userRegisterDto.getPassword())) {
            throw new IllegalArgumentException("Username, email, and password are required");
        }
        if (ObjectUtils.isEmpty(userRegisterDto.getOrgType()) || ObjectUtils.isEmpty(userRegisterDto.getOrgName())) {
            throw new IllegalArgumentException("Organization Type or Name is required");

        }
        try {

            // check if email already exists
            if (userRepository.existsByEmail(userRegisterDto.getEmail())) {
                throw new IllegalArgumentException("Email already exists");
            }

            // save org
            Organization org = new Organization();
            org.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            org.setOrgName(userRegisterDto.getOrgName());
            org.setOrgType(userRegisterDto.getOrgType());
            org.setTrustScore(0d);
            org = organizationRepository.save(org);

            User user = modelMapper.map(userRegisterDto, User.class);
            user.setCreatedAt(Timestamp.valueOf(java.time.LocalDateTime.now()));
            user.setOrganization(org);

            // set role
            Role role;
            if (roleRepository.existsByName(userRegisterDto.getRole())) {
                role = roleRepository.findByName(userRegisterDto.getRole()).get();
            } else {
                Role newRole = new Role();
                newRole.setName(userRegisterDto.getRole());
                role = roleRepository.save(newRole);
            }
            user.getRoles().add(role);
            user = userRepository.save(user);

            Department department=new Department();
            department.setDepartmentName(userRegisterDto.getDepartment());
            department.setDepartmentHead(user);
            department.setOrganization(org);
            department.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            department.getRoles().add(role);
            departmentRepository.save(department);

            // add profilePhoto to dms and set URL to user
            if (!ObjectUtils.isEmpty(profilePhoto)) {
                UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getCommonDmsUrl());
                Map<String, Object> dto = new HashMap<>();
                dto.put("userId", user.getId());
                dto.put("fileName", user.getId() + "_hr_doc");
                dto.put("remarks", "HR Doc Upload");
                dto.put("documentType", "OTHER_HR_DOCUMENTS");
                dto.put("orgId", org.getId());
                dto.put("orgType", org.getOrgType().toString());

                Map<String, Object> docPayload = new HashMap<>();
                docPayload.put("dto", dto);
                docPayload.put("file", profilePhoto);

                Map<String, String> headers = new HashMap<>();
                LoginResponse loginResponse = authenticate(new LoginRequest(webConstants.getGenericUserId(),
                        webConstants.getGenericPassword()));
                headers.put(CommonConstants.AUTHORIZATION, "Bearer " + loginResponse.getAccessToken());
                ResponseEntity<?> response = restService.iamRestCall(
                        builder.toUriString(),
                        docPayload,
                        headers,
                        HttpMethod.POST,
                        user.getId());

                if (response.getStatusCode().is2xxSuccessful()) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> respBody = (Map<String, String>) response.getBody();
                    assert respBody != null;
                    if (respBody.containsKey("documentUrl")) {
                        user.setProfilePhoto(respBody.get("documentUrl"));
                    }
                }
            }

            // prepare HR payloads
            Map<String, Object> payload = new HashMap<>();
            payload.put("employeeId", user.getId());
            payload.put("fullName", user.getName());
            payload.put("email", user.getEmail());
            payload.put("orgId", user.getOrganization().getId());
            payload.put("department", userRegisterDto.getDepartment());
            payload.put("title", userRegisterDto.getTitle());
            payload.put("remarks", "New User Registration");
            payload.put("timestamp", new Timestamp(System.currentTimeMillis()));
            payload.put("personalEmail", userRegisterDto.getPersonalEmail());
            payload.put("compensation", userRegisterDto.getCompensation());

            Map<String, String> headers = new HashMap<>();
            LoginResponse loginResponse = authenticate(new LoginRequest(webConstants.getGenericUserId(),
                    webConstants.getGenericPassword()));
            headers.put(CommonConstants.AUTHORIZATION, "Bearer " + loginResponse.getAccessToken());
            headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.APPLICATION_JSON);

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getHrInitUrl());
            ResponseEntity<?> hrResponse = restService.iamRestCall(
                    builder.toUriString(),
                    payload,
                    headers,
                    HttpMethod.POST,
                    user.getId());
            // generate JWT tokens upon registration
            if (!hrResponse.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException("Failed to initialize HR data for the user");
            }

            return registerUser(user);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to register user: " + e.getMessage());
        }
    }

    @Override
    public Map<String, String> verifyToken(String token) {
        if (token == null || !jwtUtil.validateToken(token)) {
            throw new IllegalArgumentException("Invalid token");
        }

        Claims claims = jwtUtil.extractAllClaims(token);
        Map<String, String> result = new HashMap<>();
        // return isValid, username, expiration, role
        result.put("isValid", "true");
        result.put("username", claims.getSubject());
        result.put("expiration", claims.getExpiration().toString());
        result.put("role", claims.get("role", String.class));
        return result;
    }

    @Override
    public Map<String, Object> decryptToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            // Remove "Bearer " prefix and trim whitespace
            token = token.trim();
            if (token.startsWith("Bearer ")) {
                token = token.substring(7).trim();
            }

            Claims claims = jwtUtil.extractAllClaims(token);
            Map<String, Object> decryptedData = new HashMap<>();

            // Extract all claims from the token
            decryptedData.put("username", claims.getSubject());
            decryptedData.put("issuedAt", claims.getIssuedAt());
            decryptedData.put("expiration", claims.getExpiration());
            decryptedData.put("isValid", !jwtUtil.isTokenExpired(token));

            // Extract all additional claims
            claims.forEach((key, value) -> {
                if (!key.equals("sub") && !key.equals("iat") && !key.equals("exp")) {
                    decryptedData.put(key, value);
                }
            });

            return decryptedData;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decrypt token: " + e.getMessage());
        }
    }
}

package com.nexus.iam.service.impl;

import com.nexus.iam.dto.LoginRequest;
import com.nexus.iam.dto.LoginResponse;
import com.nexus.iam.dto.UserProfileDto;
import com.nexus.iam.dto.UserRegisterDto;
import com.nexus.iam.entities.User;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.repository.OrganizationRepository;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.service.AuthenticationService;
import com.nexus.iam.service.UserService;
import com.nexus.iam.utils.CommonConstants;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final ModelMapper modelMapper;

    private final PasswordEncoder passwordEncoder;

    private final OrganizationRepository organizationRepository;

    private final RoleRepository roleRepository;

    private final WebConstants webConstants;

    private final AuthenticationService authenticationService;

    private final RestService restService;

    public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder, OrganizationRepository organizationRepository, RoleRepository roleRepository, WebConstants webConstants, AuthenticationService authenticationService, RestService restService) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.webConstants = webConstants;
        this.authenticationService = authenticationService;
        this.restService = restService;
    }

    @Override
    public ResponseEntity<?> getUserById(Long userId) {
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> {
                throw new ResourceNotFoundException("User", "id", userId);
            });
            UserRegisterDto userDto = modelMapper.map(user, UserRegisterDto.class);
            return ResponseEntity.ok(userDto);

        } catch (Exception e) {
            throw new ServiceLevelException("UserService", e.getLocalizedMessage(), "getUserById",
                    new Timestamp(System.currentTimeMillis()), e.getCause().toString(), e.getMessage());

        }
    }

    @Override
    public ResponseEntity<?> updateUser(UserRegisterDto userDto, Long userId) {
        try {
            User existingUser = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

            if (!ObjectUtils.isEmpty(userDto.getName())) {
                existingUser.setName(userDto.getName());
            }
            if (!ObjectUtils.isEmpty(userDto.getEmail())) {
                existingUser.setEmail(userDto.getEmail());
            }
            if (!ObjectUtils.isEmpty(userDto.getAddress())) {
                existingUser.setAddress(userDto.getAddress());
            }
            if (!ObjectUtils.isEmpty(userDto.getPhone())) {
                existingUser.setPhone(userDto.getPhone());
            }
            if (!ObjectUtils.isEmpty(userDto.getPassword())) {
                existingUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
            }

            // Update role if provided in the DTO
            if (!ObjectUtils.isEmpty(userDto.getRole())) {
                // Clear existing roles and add the new role
                existingUser.getRoles().clear();
                existingUser.getRoles().add(
                        roleRepository.findByName(userDto.getRole())
                                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", userDto.getRole()))
                );
            }

            userRepository.save(existingUser);
            UserRegisterDto updatedUserDto = modelMapper.map(existingUser, UserRegisterDto.class);
            return ResponseEntity.ok(updatedUserDto);

        } catch (Exception e) {
            throw new ServiceLevelException("UserService", e.getLocalizedMessage(), "updateUser",
                    new Timestamp(System.currentTimeMillis()), e.getCause().toString(), e.getMessage());

        }
    }

    @Override
    public ResponseEntity<?> deleteUser(Long userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteUser'");
    }

    @Override
    public ResponseEntity<?> getAllUsers(int page, int size) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllUsers'");
    }

    @Override
    public ResponseEntity<?> getAllEmployees(Long orgId, Integer page, Integer pageOffset) {
        try {
            // Validate pagination parameters
            if (page < 0) {
                page = 0;
            }
            if (pageOffset <= 0) {
                pageOffset = 10; // Default page size
            }

            // Create Pageable object with page number and page size
            Pageable pageable = PageRequest.of(page, pageOffset);

            // Fetch paginated users by organization ID
            var usersPage = userRepository.findByOrgId(orgId, pageable);

            // Map User entities to UserRegisterDto
            var userDtos = usersPage.map(user -> modelMapper.map(user, UserProfileDto.class));

            return ResponseEntity.ok(userDtos);
        } catch (Exception e) {
            throw new ServiceLevelException("UserService", e.getLocalizedMessage(), "getAllEmployees",
                    new Timestamp(System.currentTimeMillis()), e.getCause().toString(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> updateProfilePhoto(MultipartFile file, Long userId) {
        ResponseEntity<?> response = null;
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            if (!ObjectUtils.isEmpty(file)) {
                UriComponentsBuilder builder =
                        UriComponentsBuilder.fromUriString(webConstants.getCommonDmsUrl());

                Map<String, Object> dto = new HashMap<>();
                dto.put("userId", userId);
                dto.put("fileName", user.getId() + "_profile_photo");
                dto.put("remarks", "Profile Photo Upload");
                dto.put("documentType", "PROFILE_IMAGE");

                Map<String, Object> payload = new HashMap<>();
                payload.put("dto", dto);
                payload.put("file", file);

                Map<String, String> headers = new HashMap<>();
                LoginResponse loginResponse = authenticationService.authenticate(new LoginRequest(webConstants.getGenericUserId(),
                        webConstants.getGenericPassword()));
                headers.put(CommonConstants.AUTHORIZATION, "Bearer " + loginResponse.getAccessToken());
                headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.APPLICATION_MULTIPART_FORMDATA);

                ResponseEntity<?> dmsResponse = restService.iamRestCall(
                        builder.toUriString(),
                        payload,
                        headers,
                        HttpMethod.POST,
                        userId
                );

                if (dmsResponse.getStatusCode().is2xxSuccessful()) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> respBody = (Map<String, String>) dmsResponse.getBody();
                    assert respBody != null;
                    if (respBody.containsKey("documentUrl")) {
                        user.setProfilePhoto(respBody.get("documentUrl"));
                        userRepository.save(user);

                        response = new ResponseEntity<>("Profile photo updated successfully", HttpStatus.OK);
                    } else {
                        response = new ResponseEntity<>("Failed to retrieve document URL from DMS response", HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                } else {
                    response = new ResponseEntity<>("Failed to upload profile photo to DMS: " + dmsResponse.getBody(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        } catch (Exception e) {
            throw new ServiceLevelException("UserService", e.getLocalizedMessage(), "updateProfilePhoto",
                    new Timestamp(System.currentTimeMillis()), e.getCause().toString(), e.getMessage());
        }

        return response;
    }

    @Override
    public ResponseEntity<?> createUser(UserProfileDto userDto) {
        try {
            User user = modelMapper.map(userDto, User.class);
            user.setCreatedAt(new Timestamp(System.currentTimeMillis()));

            // Set organization
            user.setOrganization(organizationRepository.findById(userDto.getOrgId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", userDto.getOrgId())));

            // Set role
            user.getRoles().add(roleRepository.findByName(userDto.getRole())
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", userDto.getRole())));

            // Generate a random password
            String generatedPassword = generateRandomPassword();
            user.setPassword(passwordEncoder.encode(generatedPassword));

            // Set default user status
            user.setEnabled(true);
            user.setAccountNonExpired(true);
            user.setAccountNonLocked(true);
            user.setCredentialsNonExpired(true);

            // Save user to repository
            userRepository.save(user);

            // Return email and password instead of JWT
            Map<String, String> response = new HashMap<>();
            response.put("email", user.getEmail());
            response.put("userId", user.getId().toString());
            response.put("password", generatedPassword);
            response.put("message", "User created successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new ServiceLevelException("UserService", e.getLocalizedMessage(), "createUser",
                    new Timestamp(System.currentTimeMillis()), null, e.getMessage());
        }
    }

    /**
     * Generates a random password with mixed case letters, numbers and special
     * characters
     *
     * @return generated password
     */
    private String generateRandomPassword() {
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String specialChars = "!@#$%^&*";
        String allChars = uppercase + lowercase + digits + specialChars;

        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();

        // Ensure at least one character from each category
        password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));

        // Fill remaining characters randomly
        for (int i = 4; i < 12; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the password
        String[] passwordArray = password.toString().split("");
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int randomIndex = random.nextInt(i + 1);
            String temp = passwordArray[i];
            passwordArray[i] = passwordArray[randomIndex];
            passwordArray[randomIndex] = temp;
        }

        return String.join("", passwordArray);
    }
}

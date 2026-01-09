package com.nexus.iam.service.impl;


import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nexus.iam.dto.UserProfileDto;
import com.nexus.iam.dto.UserRegisterDto;
import com.nexus.iam.entities.User;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
            User existingUser = userRepository.findById(userId).orElseThrow(() -> {
                throw new ResourceNotFoundException("User", "id", userId);
            });

            existingUser.setName(userDto.getName());
            existingUser.setEmail(userDto.getEmail());
            existingUser.setAddress(userDto.getAddress());
            existingUser.setPhone(userDto.getPhone());

            // Add other fields as necessary

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
            var userDtos = usersPage.map(user -> modelMapper.map(user, UserRegisterDto.class));

            return ResponseEntity.ok(userDtos);
        } catch (Exception e) {
            throw new ServiceLevelException("UserService", e.getLocalizedMessage(), "getAllEmployees",
                    new Timestamp(System.currentTimeMillis()), e.getCause().toString(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> createUser(UserProfileDto userDto) {
        try {
            User user = modelMapper.map(userDto, User.class);
            user.setCreatedAt(new Timestamp(System.currentTimeMillis()));

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
            response.put("message", "User created successfully. Please note: people_id and organization_id should be set separately through the People entity");

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

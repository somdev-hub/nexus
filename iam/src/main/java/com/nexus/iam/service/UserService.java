package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;

import com.nexus.iam.dto.UserProfileDto;
import com.nexus.iam.dto.UserRegisterDto;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    ResponseEntity<?> getUserById(Long userId);

    ResponseEntity<?> createUser(UserProfileDto userDto, MultipartFile[] files);

    ResponseEntity<?> updateUser(UserRegisterDto userDto, Long userId);

    ResponseEntity<?> deleteUser(Long userId);

    ResponseEntity<?> getAllUsers(int page, int size);

    ResponseEntity<?> getAllEmployees(Long orgId, Integer page, Integer pageOffset);

    ResponseEntity<?> updateProfilePhoto(MultipartFile file, Long userId);

    ResponseEntity<?> getUserDetails(Long userId);
}

package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;

import com.nexus.iam.dto.UserRegisterDto;

public interface UserService {
    ResponseEntity<?> getUserById(Long userId);

    ResponseEntity<?> updateUser(UserRegisterDto userDto, Long userId);

    ResponseEntity<?> deleteUser(Long userId);

    ResponseEntity<?> getAllUsers(int page, int size);

}

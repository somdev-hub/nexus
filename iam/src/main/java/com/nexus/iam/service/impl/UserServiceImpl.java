package com.nexus.iam.service.impl;

import java.sql.Timestamp;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.nexus.iam.dto.UserRegisterDto;
import com.nexus.iam.entities.User;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.service.UserService;

public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateUser'");
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

}

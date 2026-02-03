package com.nexus.iam.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.iam.dto.LoginRequest;
import com.nexus.iam.dto.LoginResponse;
import com.nexus.iam.dto.RefreshTokenRequest;
import com.nexus.iam.dto.UserRegisterDto;

@Service
public interface AuthenticationService {
    public LoginResponse authenticate(LoginRequest loginRequest);

    public LoginResponse refreshToken(RefreshTokenRequest refreshToken);

    public LoginResponse registerUser(UserRegisterDto userRegisterDto, MultipartFile profilePhoto);

    public Map<String, String> verifyToken(String token);

    public Map<String, Object> decryptToken(String token);

}

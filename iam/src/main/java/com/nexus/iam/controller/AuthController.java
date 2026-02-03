package com.nexus.iam.controller;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.dto.DecryptTokenRequest;
import com.nexus.iam.dto.LoginRequest;
import com.nexus.iam.dto.LoginResponse;
import com.nexus.iam.dto.RefreshTokenRequest;
import com.nexus.iam.dto.UserRegisterDto;
import com.nexus.iam.service.AuthenticationService;
import com.nexus.iam.utils.CommonConstants;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/iam/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @LogActivity("Login Attempt")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (ObjectUtils.isEmpty(loginRequest)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Login request cannot be empty");
        }
        LoginResponse response = authenticationService.authenticate(loginRequest);
        return ResponseEntity.ok(response);

    }

    @LogActivity("Refresh Token Attempt")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        LoginResponse response = authenticationService.refreshToken(refreshTokenRequest);
        return ResponseEntity.ok(response);
    }

    @LogActivity("User Registration Attempt")
    @PostMapping(name = "/register", consumes = CommonConstants.APPLICATION_MULTIPART_FORMDATA)
    public ResponseEntity<?> registerUser(@RequestPart(name = "dto", required = true) UserRegisterDto userRegisterDto,
            @RequestPart(name = "profilePhoto", required = false) MultipartFile profilePhoto) {
        LoginResponse registerUser = authenticationService.registerUser(userRegisterDto, profilePhoto);
        return ResponseEntity.status(HttpStatus.CREATED).body(registerUser);
    }

    @LogActivity("Token Verification Attempt")
    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestBody Map<String, String> token) {
        if (ObjectUtils.isEmpty(token) || !token.containsKey("token")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token is required");
        }
        Map<String, String> result = authenticationService.verifyToken(token.get("token"));
        return ResponseEntity.ok(result);
    }

    @LogActivity("Token Decryption Attempt")
    @PostMapping("/decrypt")
    public ResponseEntity<?> decryptToken(@RequestBody DecryptTokenRequest request) {
        Map<String, Object> decryptedToken = authenticationService.decryptToken(request.getToken());
        return ResponseEntity.ok(decryptedToken);
    }

}

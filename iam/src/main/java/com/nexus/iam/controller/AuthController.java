package com.nexus.iam.controller;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.dto.*;
import com.nexus.iam.entities.User;
import com.nexus.iam.service.AuthenticationService;
import com.nexus.iam.utils.WebConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/iam/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final WebConstants webConstants;

    public AuthController(AuthenticationService authenticationService, WebConstants webConstants) {
        this.authenticationService = authenticationService;
        this.webConstants = webConstants;
    }

    @LogActivity("Create Default Credentials Attempt")
    @PostMapping("/create-default-credentials")
    public ResponseEntity<?> createDefaultCredentials() {
        try {
            String email = webConstants.getGenericUserId();
            String password = webConstants.getGenericPassword();
            if (ObjectUtils.isEmpty(email) || ObjectUtils.isEmpty(password)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Default credentials are not set in the configuration");
            }
            User user = new User();
            user.setEmail(email);
            user.setPassword(password);
            LoginResponse response = authenticationService.registerUser(user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating default credentials: " + e.getMessage());
        }
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
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerUser(@RequestPart(name = "dto", required = true) UserRegisterDto userRegisterDto,
                                          @RequestPart(name = "profilePicture", required = false) MultipartFile profilePhoto) {
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

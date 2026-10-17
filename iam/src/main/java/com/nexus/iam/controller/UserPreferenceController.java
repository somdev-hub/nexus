package com.nexus.iam.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.dto.SaveUserPreferencesDto;
import com.nexus.iam.security.JwtUtil;
import com.nexus.iam.service.UserPreferenceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/iam/user-preferences")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<?> getPreferences(
            @RequestParam(value = "settingName") String settingName,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        ResponseEntity<?> unauthorized = unauthorized(authHeader);
        if (unauthorized != null) {
            return unauthorized;
        }
        return userPreferenceService.getPreferences(resolveUserId(authHeader, userId), settingName);
    }

    @LogActivity("Save User Preferences")
    @PostMapping
    public ResponseEntity<?> savePreferences(
            @RequestBody SaveUserPreferencesDto request,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        ResponseEntity<?> unauthorized = unauthorized(authHeader);
        if (unauthorized != null) {
            return unauthorized;
        }
        return userPreferenceService.savePreferences(resolveUserId(authHeader, userId), request);
    }

    private ResponseEntity<?> unauthorized(String authHeader) {
        if (ObjectUtils.isEmpty(authHeader) || !jwtUtil.isValidToken(authHeader)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid or missing token");
        }
        return null;
    }

    /**
     * Prefer the user id embedded in the token (not spoofable), falling back to
     * the explicit {@code userId} param for tokens that do not carry a numeric
     * {@code userId} claim.
     */
    private Long resolveUserId(String authHeader, Long fallbackUserId) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        Long tokenUserId = jwtUtil.extractUserIdFromToken(token);
        return tokenUserId != null ? tokenUserId : fallbackUserId;
    }
}

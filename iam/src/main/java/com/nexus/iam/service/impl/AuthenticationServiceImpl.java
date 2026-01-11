package com.nexus.iam.service.impl;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.exception.ResourceNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.nexus.iam.dto.LoginRequest;
import com.nexus.iam.dto.LoginResponse;
import com.nexus.iam.dto.RefreshTokenRequest;
import com.nexus.iam.dto.UserRegisterDto;
import com.nexus.iam.entities.User;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.security.JwtUtil;
import com.nexus.iam.service.AuthenticationService;

import io.jsonwebtoken.Claims;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private RoleRepository roleRepository;

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

    public LoginResponse registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        try {
            userRepository.save(user);

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
                    .userId(
                            userRepository.findByEmail(user.getUsername()).map(
                                    User::getId).orElse(null)

                    )
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to register user: " + e.getMessage());
        }
    }

    @Override
    public LoginResponse registerUser(UserRegisterDto userRegisterDto) {
        if (ObjectUtils.isEmpty(userRegisterDto)) {
            throw new IllegalArgumentException("User registration data cannot be null or empty");
        }
        if (ObjectUtils.isEmpty(userRegisterDto.getName()) ||
                ObjectUtils.isEmpty(userRegisterDto.getEmail()) ||
                ObjectUtils.isEmpty(userRegisterDto.getPassword())) {
            throw new IllegalArgumentException("Username, email, and password are required");
        }
        try {
            User user = modelMapper.map(userRegisterDto, User.class);
            user.setCreatedAt(Timestamp.valueOf(java.time.LocalDateTime.now()));

            // Set role if provided in the DTO
            if (!ObjectUtils.isEmpty(userRegisterDto.getRole())) {
                user.getRoles().add(roleRepository.findByName(userRegisterDto.getRole())
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", userRegisterDto.getRole())));
            }

            return registerUser(user);
            // generate JWT tokens upon registration

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

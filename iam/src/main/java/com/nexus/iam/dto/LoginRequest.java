package com.nexus.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String email;
    private String password;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApplicantLoginRequest{
        private String personalEmail;
        private String password;
    }
}

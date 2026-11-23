package com.nexus.iam.dto;

import lombok.Data;

@Data
public class UserDetailsDto {

    private Long id;

    private String name;

    private String email;

    private String role;

    private String department;

    private String phone;

    private String address;

    private String profilePhoto;

    private String personalEmail;

    // Inter-org chat support
    private Long orgId;
    private String orgName;
    private String orgType;
}

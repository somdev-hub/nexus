package com.nexus.iam.dto;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class UserProfileDto {
    private String name;

    private String email;

    private String phone;

    private String address;

    private Double salary;

    private Timestamp joiningDate;

    private String notes;

    private Timestamp createdAt;

    private String profilePhoto;

    private Long orgId;

    private String role;
}

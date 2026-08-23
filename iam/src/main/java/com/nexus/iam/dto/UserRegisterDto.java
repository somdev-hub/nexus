package com.nexus.iam.dto;

import java.sql.Date;

import com.nexus.iam.entities.OrgType;

import lombok.Data;

@Data
public class UserRegisterDto {
    private String name;

    private String email;

    private String phone;

    private String address;

    private String profilePhoto;

    private Long orgId;

    private String role;

    private CompensationDto compensation;

    private String personalEmail;

    private String department;

    private String title;

    private String password;

    private String gender;

    private Integer age;

    private Date dateOfBirth;

    private String orgName;

    private OrgType orgType;
}

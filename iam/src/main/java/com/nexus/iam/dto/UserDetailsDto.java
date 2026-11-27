package com.nexus.iam.dto;

import com.nexus.iam.entities.Gender;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

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

    private Gender gender;

    private Integer age;

    private Date dateOfBirth;

    private String notes;

    private Timestamp createdAt;

    // Inter-org chat support
    private Long orgId;
    private String orgName;
    private String orgType;
}

package com.nexus.iam.dto;

import java.sql.Date;
import java.sql.Timestamp;

import com.nexus.iam.entities.Gender;
import lombok.Data;

@Data
public class UserProfileDto {
    private String name;

    private String email;

    private String phone;

    private String address;

    private String notes;

    private String profilePhoto;

    private Long orgId;

    private String role;

    private Timestamp effectiveFrom;

    private CompensationDto compensation;

    private String personalEmail;

    private String department;

    private Long deptId;

    private boolean isDeptHead;

    private String title;

    private String remarks;

    private Gender gender;

    private Integer age;

    private Date dateOfBirth;
}

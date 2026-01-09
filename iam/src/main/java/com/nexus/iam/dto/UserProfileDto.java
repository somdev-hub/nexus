package com.nexus.iam.dto;

import java.sql.Time;
import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    private Long peopleId;

    private Long organizationId;
}

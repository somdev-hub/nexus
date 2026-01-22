package com.nexus.iam.dto;

import java.sql.Timestamp;
import java.util.List;

import lombok.Data;
import org.json.JSONObject;

@Data
public class UserProfileDto {
    private String name;

    private String email;

    private String phone;

    private String address;

    private String notes;

    private Timestamp createdAt;

    private String profilePhoto;

    private Long orgId;

    private String role;

    private Timestamp effectiveFrom;

    private JSONObject compensation;

    private String personalEmail;

    private String department;

    private String title;

    private String remarks;
}

package com.nexus.iam.dto;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class UserRegisterDto {
    private String name;

    private String email;

    private String password;

    private String phone;

    private String address;

    private Timestamp createdAt;

    private String profilePhoto;
}

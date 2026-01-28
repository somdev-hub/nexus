package com.nexus.iam.dto;

import lombok.Data;

@Data
public class UserDetailsDto {

    private Long id;

    private String name;

    private String email;

    private String phone;

    private String address;

    private String profilePhoto;

    private String personalEmail;
}

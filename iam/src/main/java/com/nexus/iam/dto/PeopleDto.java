package com.nexus.iam.dto;

import com.nexus.iam.entities.Role;

import lombok.Data;

@Data
public class PeopleDto {
    private UserProfileDto user;

    private Role role;
}

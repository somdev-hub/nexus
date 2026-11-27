package com.nexus.iam.dto;

import java.sql.Date;

import com.nexus.iam.entities.Gender;
import lombok.Data;

@Data
public class UpdateUserProfileDto {
    private String name;
    private String phone;
    private String address;
    private String personalEmail;
    private Gender gender;
    private Date dateOfBirth;
}

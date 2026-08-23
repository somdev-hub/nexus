package com.nexus.iam.dto;

import com.nexus.iam.entities.Gender;
import lombok.Data;

import java.sql.Date;

@Data
public class AdminRegisterDto {
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private Gender gender;
    private Integer age;
    private Date dateOfBirth;
    private String password;
}

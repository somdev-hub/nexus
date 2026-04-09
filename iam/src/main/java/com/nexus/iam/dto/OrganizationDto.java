package com.nexus.iam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nexus.iam.entities.OrgType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {

    @JsonProperty(value = "orgId")
    private Long id;

    private String orgName;

    private OrgType orgType;

    private Double trustScore;

    private Timestamp createdAt;

    private String orgEmail;

    private String orgPhone;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String pinCode;

    private String country;
}

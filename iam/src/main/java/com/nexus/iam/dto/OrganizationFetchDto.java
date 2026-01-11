package com.nexus.iam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nexus.iam.entities.OrgType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
public class OrganizationFetchDto {
    @JsonProperty(value = "orgId")
    private Long id;

    private String orgName;

    private OrgType orgType;

    private Double trustScore;

    private Timestamp createdAt;

    private Long employeeCount;
}

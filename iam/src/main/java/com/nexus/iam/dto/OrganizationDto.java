package com.nexus.iam.dto;

import com.nexus.iam.entities.OrgType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {
    private String orgName;

    private OrgType orgType;

    private Double trustScore;

    private Timestamp createdAt;

    private List<PeopleDto> people;
}

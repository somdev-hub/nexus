package com.nexus.iam.dto;

import com.nexus.iam.entities.OrgType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationDirectoryDto {
    private Long id;

    private String orgName;

    private OrgType orgType;

    private String city;

    private String country;

    private Double trustScore;
}

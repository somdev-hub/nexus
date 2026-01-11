package com.nexus.iam.service;

import com.nexus.iam.dto.OrganizationDto;
import com.nexus.iam.dto.OrganizationFetchDto;
import com.nexus.iam.entities.Organization;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface OrganizationService {
    OrganizationDto createOrganization(OrganizationDto organizationDto, Long member);

    OrganizationFetchDto getOrganizationById(Long id);

    List<Organization> getAllOrganizations();

    OrganizationDto updateOrganization(Long id, OrganizationDto organizationDto);

    void deleteOrganization(Long id);

    OrganizationDto getOrganizationByName(String orgName);

    void assignMemberToOrganization(Long orgId, Long memberId);

    void removeMemberFromOrganization(Long orgId, Long memberId);
}

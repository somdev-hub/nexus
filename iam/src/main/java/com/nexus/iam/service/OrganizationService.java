package com.nexus.iam.service;

import com.nexus.iam.dto.OrganizationDto;
import com.nexus.iam.entities.Organization;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface OrganizationService {
    Organization createOrganization(OrganizationDto organizationDto, Long member);

    Organization getOrganizationById(Long id);

    List<Organization> getAllOrganizations();

    Organization updateOrganization(Long id, OrganizationDto organizationDto);

    void deleteOrganization(Long id);

    Organization getOrganizationByName(String orgName);

    void assignMemberToOrganization(Long orgId, Long memberId);

    void removeMemberFromOrganization(Long orgId, Long memberId);
}

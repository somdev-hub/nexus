package com.nexus.iam.service.impl;

import com.nexus.iam.dto.OrganizationDto;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.entities.People;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.repository.OrganizationRepository;
import com.nexus.iam.repository.PeopleRepository;
import com.nexus.iam.service.OrganizationService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PeopleRepository peopleRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public Organization createOrganization(OrganizationDto organizationDto, Long member) {
        if (ObjectUtils.isEmpty(organizationDto)) {
            throw new IllegalArgumentException("Organization data cannot be null or empty");
        }
        if (ObjectUtils.isEmpty(organizationDto.getOrgName())) {
            throw new IllegalArgumentException("Organization name is required");
        }
        if (organizationRepository.existsByOrgName(organizationDto.getOrgName())) {
            throw new IllegalArgumentException(
                    "Organization with name already exists: " + organizationDto.getOrgName());
        }

        Organization organization = modelMapper.map(organizationDto, Organization.class);
        organization.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        People people = peopleRepository.findById(member).get();
        organization.getPeople().add(people);
        return organizationRepository.save(organization);
    }

    @Override
    public Organization getOrganizationById(Long id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
    }

    @Override
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    @Override
    public Organization updateOrganization(Long id, OrganizationDto organizationDto) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        if (ObjectUtils.isEmpty(organizationDto)) {
            throw new IllegalArgumentException("Organization data cannot be null or empty");
        }

        Organization organization = getOrganizationById(id);

        if (!ObjectUtils.isEmpty(organizationDto.getOrgName()) &&
                !organizationDto.getOrgName().equals(organization.getOrgName()) &&
                organizationRepository.existsByOrgName(organizationDto.getOrgName())) {
            throw new IllegalArgumentException(
                    "Organization with name already exists: " + organizationDto.getOrgName());
        }

        modelMapper.map(organizationDto, organization);
        return organizationRepository.save(organization);
    }

    @Override
    public void deleteOrganization(Long id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        Organization organization = getOrganizationById(id);
        organizationRepository.delete(organization);
    }

    @Override
    public Organization getOrganizationByName(String orgName) {
        if (ObjectUtils.isEmpty(orgName)) {
            throw new IllegalArgumentException("Organization name cannot be null");
        }
        return organizationRepository.findByOrgName(orgName)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with name: " + orgName));
    }

    @Override
    public void assignMemberToOrganization(Long orgId, Long memberId) {
        Organization organization = getOrganizationById(orgId);
        People people = peopleRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId));
        organization.getPeople().add(people);
        organizationRepository.save(organization);
    }

    @Override
    public void removeMemberFromOrganization(Long orgId, Long memberId) {
        Organization organization = getOrganizationById(orgId);
        People people = peopleRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId));
        organization.getPeople().remove(people);
        organizationRepository.save(organization);
    }
}

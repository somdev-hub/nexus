package com.nexus.iam.service.impl;

import com.nexus.iam.dto.OrganizationDto;
import com.nexus.iam.dto.OrganizationFetchDto;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.entities.Role;
import com.nexus.iam.entities.User;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.repository.OrganizationRepository;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.service.OrganizationService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;

    private final ModelMapper modelMapper;

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    public OrganizationServiceImpl(OrganizationRepository organizationRepository, ModelMapper modelMapper, RoleRepository roleRepository, UserRepository userRepository) {
        this.organizationRepository = organizationRepository;
        this.modelMapper = modelMapper;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public OrganizationDto createOrganization(OrganizationDto organizationDto, Long userId) {
        if (ObjectUtils.isEmpty(organizationDto)) {
            throw new IllegalArgumentException("Organization data cannot be null or empty");
        }
        if (ObjectUtils.isEmpty(organizationDto.getOrgName())) {
            throw new IllegalArgumentException("Organization name is required");
        }
        if (Boolean.TRUE.equals(organizationRepository.existsByOrgName(organizationDto.getOrgName()))) {
            throw new IllegalArgumentException(
                    "Organization with name already exists: " + organizationDto.getOrgName());
        }

        // Get the user - fail early if user doesn't exist
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Verify DIRECTOR role exists - fail early if role doesn't exist
        var directorRole = roleRepository.findByName("DIRECTOR")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "DIRECTOR"));

        // Create the Organization only after all validations pass
        Organization organization = modelMapper.map(organizationDto, Organization.class);
        organization.setUsers(new ArrayList<>());
        organization.setDocuments(new ArrayList<>());
        organization.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        Organization savedOrganization = organizationRepository.save(organization);

        // Assign user to this organization with DIRECTOR role
        user.setOrganization(savedOrganization);
        user.getRoles().add(directorRole);

        userRepository.save(user);

        return modelMapper.map(savedOrganization, OrganizationDto.class);
    }

    @Override
    public OrganizationFetchDto getOrganizationById(Long id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        return organizationRepository.fetchByOrgId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
    }

    @Override
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    @Override
    public OrganizationDto updateOrganization(Long id, OrganizationDto organizationDto) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        if (ObjectUtils.isEmpty(organizationDto)) {
            throw new IllegalArgumentException("Organization data cannot be null or empty");
        }

        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));

        if (Boolean.TRUE.equals(organizationRepository.existsByOrgName(organizationDto.getOrgName())) && !ObjectUtils.isEmpty(organizationDto.getOrgName()) && !organizationDto.getOrgName().equals(organization.getOrgName())) {
                throw new IllegalArgumentException(
                        "Organization with name already exists: " + organizationDto.getOrgName());
            }

        modelMapper.map(organizationDto, organization);
        Organization updatedOrganization = organizationRepository.save(organization);
        return modelMapper.map(updatedOrganization, OrganizationDto.class);
    }

    @Override
    public void deleteOrganization(Long id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
        organizationRepository.delete(organization);
    }

    @Override
    public OrganizationDto getOrganizationByName(String orgName) {
        if (ObjectUtils.isEmpty(orgName)) {
            throw new IllegalArgumentException("Organization name cannot be null or empty");
        }
        Organization organization = organizationRepository.findByOrgName(orgName)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "name", orgName));
        return modelMapper.map(organization, OrganizationDto.class);
    }

    @Override
    public void assignMemberToOrganization(Long orgId, Long userId) {
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Set organization for the user
        user.setOrganization(organization);

        // Add MEMBER role if not already present
        user.getRoles().add(roleRepository.findByName("MEMBER")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "MEMBER")));

        userRepository.save(user);
    }

    @Override
    public void removeMemberFromOrganization(Long orgId, Long userId) {
        // Verify organization exists
        organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Remove organization from user
        if (user.getOrganization() != null && user.getOrganization().getId().equals(orgId)) {
            user.setOrganization(null);
            userRepository.save(user);
        }
    }

    @Override
    public Map<String, Object> getUserOrganizationDetails(Long userId) {
        try {
            Map<String, Object> result = new HashMap<>();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            Organization organization = user.getOrganization();
            if (organization == null) {
                throw new ResourceNotFoundException("Organization", "userId", userId);
            }
            result.put("orgName", organization.getOrgName());
            result.put("orgId", organization.getId());
            result.put("userRoles", user.getRoles().stream().map(Role::getName).toList());
            result.put("orgType", organization.getOrgType());
            return result;
        } catch (ServiceLevelException e) {
            throw new ServiceLevelException(
                    "OrganizationService", "Failed to get user organization details", "getUserOrganizationDetails",
                    e.getClass().getSimpleName(), e.getLocalizedMessage()
            );
        }
    }
}

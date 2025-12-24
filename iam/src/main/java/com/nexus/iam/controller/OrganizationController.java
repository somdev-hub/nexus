package com.nexus.iam.controller;

import com.nexus.iam.dto.ErrorResponseDto;
import com.nexus.iam.dto.OrganizationDto;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.service.OrganizationService;
import com.nexus.iam.utils.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/iam/organizations")
@CrossOrigin(origins = "*")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private Logger logger;

    @PostMapping("/add")
    public ResponseEntity<?> createOrganization(@RequestBody OrganizationDto organizationDto,
            @RequestParam Long member) {

        if (ObjectUtils.isEmpty(member)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Member ID is required");
        }
        if (ObjectUtils.isEmpty(organizationDto) || ObjectUtils.isEmpty(organizationDto.getOrgName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization data is required");
        }

        ResponseEntity<?> response = null;
        try {
            OrganizationDto organization = organizationService.createOrganization(organizationDto, member);
            response = ResponseEntity.status(HttpStatus.CREATED).body(organization);
        } catch (Exception e) {
            response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDto(
                    "Bad Request",
                    HttpStatus.BAD_REQUEST.value(),
                    null,
                    e.getMessage()));
        } finally {
            logger.log("/iam/organization/add", HttpMethod.POST,
                    response != null ? response.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR, organizationDto,
                    response != null ? response.getBody() : null, member);
        }

        return response;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrganizationById(@PathVariable Long id) {

        try {
            OrganizationDto organization = organizationService.getOrganizationById(id);
            return ResponseEntity.ok(organization);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/")
    public ResponseEntity<?> getAllOrganizations() {
        try {
            List<Organization> organizations = organizationService.getAllOrganizations();
            return ResponseEntity.ok(organizations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/name/{orgName}")
    public ResponseEntity<?> getOrganizationByName(@PathVariable String orgName) {
        try {
            OrganizationDto organization = organizationService.getOrganizationByName(orgName);
            return ResponseEntity.ok(organization);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrganization(@PathVariable Long id, @RequestBody OrganizationDto organizationDto) {
        try {
            OrganizationDto organization = organizationService.updateOrganization(id, organizationDto);
            return ResponseEntity.ok(organization);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrganization(@PathVariable Long id) {
        try {
            organizationService.deleteOrganization(id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}

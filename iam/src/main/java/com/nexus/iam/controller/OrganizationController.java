package com.nexus.iam.controller;

import com.nexus.iam.dto.OrganizationDto;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/iam/organizations")
@CrossOrigin(origins = "*")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<?> createOrganization(@RequestBody OrganizationDto organizationDto, @RequestParam Long member) {
        try {
            Organization organization = organizationService.createOrganization(organizationDto,member);
            return ResponseEntity.status(HttpStatus.CREATED).body(organization);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrganizationById(@PathVariable Long id) {
        try {
            Organization organization = organizationService.getOrganizationById(id);
            return ResponseEntity.ok(organization);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping
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
            Organization organization = organizationService.getOrganizationByName(orgName);
            return ResponseEntity.ok(organization);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrganization(@PathVariable Long id, @RequestBody OrganizationDto organizationDto) {
        try {
            Organization organization = organizationService.updateOrganization(id, organizationDto);
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

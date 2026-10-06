package com.nexus.hr.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.hr.annotation.LogActivity;
import com.nexus.hr.model.entities.OrgAccountInfo;
import com.nexus.hr.model.entities.OrgAddress;
import com.nexus.hr.service.interfaces.OrgService;
import com.nexus.hr.utils.CommonUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hr/orgs")
public class OrgController {

    private final OrgService orgService;
    private final CommonUtils commonUtils;

    @LogActivity("Create Organization Account Information")
    @PostMapping("/{orgId}/account-info")
    public ResponseEntity<?> createOrgAccountInfo(@PathVariable Long orgId, @RequestBody OrgAccountInfo orgAccountInfo,
            @RequestHeader("Authorization") String auth) {
        if (ObjectUtils.isEmpty(auth) || !commonUtils.validateToken(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }
        return orgService.createOrgAccountInfo(orgId, orgAccountInfo, auth);
    }

    @LogActivity("Get Organization Account Information")
    @GetMapping("/{orgId}/account-info")
    public ResponseEntity<?> getOrgAccountInfo(@PathVariable Long orgId, @RequestHeader("Authorization") String auth) {
        if (ObjectUtils.isEmpty(auth) || !commonUtils.validateToken(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }
        return orgService.getOrgAccountInfo(orgId, auth);
    }

    @LogActivity("Update Organization Account Information")
    @PutMapping("/{orgId}/account-info")
    public ResponseEntity<?> updateOrgAccountInfo(@PathVariable Long orgId, @RequestBody OrgAccountInfo orgAccountInfo,
            @RequestHeader("Authorization") String auth) {
        if (ObjectUtils.isEmpty(auth) || !commonUtils.validateToken(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }
        return orgService.updateOrgAccountInfo(orgId, orgAccountInfo, auth);
    }

    @LogActivity("Delete Organization Account Information")
    @DeleteMapping("/{orgId}/account-info")
    public ResponseEntity<?> deleteOrgAccountInfo(@PathVariable Long orgId,
            @RequestHeader("Authorization") String auth) {
        if (ObjectUtils.isEmpty(auth) || !commonUtils.validateToken(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }
        return orgService.deleteOrgAccountInfo(orgId, auth);
    }

    @LogActivity("Create Organization Address")
    @PostMapping("/{orgId}/addresses")
    public ResponseEntity<?> createOrgAddress(@PathVariable Long orgId, @RequestBody OrgAddress orgAddress,
            @RequestHeader("Authorization") String auth) {
        if (ObjectUtils.isEmpty(auth) || !commonUtils.validateToken(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }
        return orgService.createOrgAddress(orgId, orgAddress, auth);
    }

    @LogActivity("Get Organization Addresses")
    @GetMapping("/{orgId}/addresses")
    public ResponseEntity<?> getOrgAddresses(@PathVariable Long orgId,
            @RequestHeader("Authorization") String auth) {
        if (ObjectUtils.isEmpty(auth) || !commonUtils.validateToken(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }
        return orgService.getOrgAddresses(orgId, auth);
    }

    @LogActivity("Get Organization Address")
    @GetMapping("/{orgId}/addresses/{addressId}")
    public ResponseEntity<?> getOrgAddress(@PathVariable Long orgId, @PathVariable Long addressId,
            @RequestHeader("Authorization") String auth) {
        if (ObjectUtils.isEmpty(auth) || !commonUtils.validateToken(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }
        return orgService.getOrgAddress(orgId, addressId, auth);
    }

    @LogActivity("Update Organization Address")
    @PutMapping("/{orgId}/addresses/{addressId}")
    public ResponseEntity<?> updateOrgAddress(@PathVariable Long orgId, @PathVariable Long addressId,
            @RequestBody OrgAddress orgAddress, @RequestHeader("Authorization") String auth) {
        if (ObjectUtils.isEmpty(auth) || !commonUtils.validateToken(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }
        return orgService.updateOrgAddress(orgId, addressId, orgAddress, auth);
    }

    @LogActivity("Delete Organization Address")
    @DeleteMapping("/{orgId}/addresses/{addressId}")
    public ResponseEntity<?> deleteOrgAddress(@PathVariable Long orgId, @PathVariable Long addressId,
            @RequestHeader("Authorization") String auth) {
        if (ObjectUtils.isEmpty(auth) || !commonUtils.validateToken(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }
        return orgService.deleteOrgAddress(orgId, addressId, auth);
    }

}

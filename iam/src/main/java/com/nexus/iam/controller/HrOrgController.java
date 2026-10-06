package com.nexus.iam.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.service.HrOrgService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/iam/hr/orgs")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class HrOrgController {

    private final HrOrgService hrOrgService;

    @LogActivity("Create Org Account Info via IAM")
    @PostMapping("/{orgId}/account-info")
    public ResponseEntity<?> createAccountInfo(@PathVariable Long orgId, @RequestBody Map<String, Object> dto,
            @RequestHeader("Authorization") String auth) {
        return hrOrgService.createOrgAccountInfo(orgId, dto, auth);
    }

    @LogActivity("Get Org Account Info via IAM")
    @GetMapping("/{orgId}/account-info")
    public ResponseEntity<?> getAccountInfo(@PathVariable Long orgId,
            @RequestHeader("Authorization") String auth) {
        return hrOrgService.getOrgAccountInfo(orgId, auth);
    }

    @LogActivity("Update Org Account Info via IAM")
    @PutMapping("/{orgId}/account-info")
    public ResponseEntity<?> updateAccountInfo(@PathVariable Long orgId, @RequestBody Map<String, Object> dto,
            @RequestHeader("Authorization") String auth) {
        return hrOrgService.updateOrgAccountInfo(orgId, dto, auth);
    }

    @LogActivity("Delete Org Account Info via IAM")
    @DeleteMapping("/{orgId}/account-info")
    public ResponseEntity<?> deleteAccountInfo(@PathVariable Long orgId,
            @RequestHeader("Authorization") String auth) {
        return hrOrgService.deleteOrgAccountInfo(orgId, auth);
    }

    @LogActivity("Create Org Address via IAM")
    @PostMapping("/{orgId}/addresses")
    public ResponseEntity<?> createAddress(@PathVariable Long orgId, @RequestBody Map<String, Object> dto,
            @RequestHeader("Authorization") String auth) {
        return hrOrgService.createOrgAddress(orgId, dto, auth);
    }

    @LogActivity("Get Org Addresses via IAM")
    @GetMapping("/{orgId}/addresses")
    public ResponseEntity<?> getAddresses(@PathVariable Long orgId,
            @RequestHeader("Authorization") String auth) {
        return hrOrgService.getOrgAddresses(orgId, auth);
    }

    @LogActivity("Get Org Address via IAM")
    @GetMapping("/{orgId}/addresses/{addressId}")
    public ResponseEntity<?> getAddress(@PathVariable Long orgId, @PathVariable Long addressId,
            @RequestHeader("Authorization") String auth) {
        return hrOrgService.getOrgAddress(orgId, addressId, auth);
    }

    @LogActivity("Update Org Address via IAM")
    @PutMapping("/{orgId}/addresses/{addressId}")
    public ResponseEntity<?> updateAddress(@PathVariable Long orgId, @PathVariable Long addressId,
            @RequestBody Map<String, Object> dto, @RequestHeader("Authorization") String auth) {
        return hrOrgService.updateOrgAddress(orgId, addressId, dto, auth);
    }

    @LogActivity("Delete Org Address via IAM")
    @DeleteMapping("/{orgId}/addresses/{addressId}")
    public ResponseEntity<?> deleteAddress(@PathVariable Long orgId, @PathVariable Long addressId,
            @RequestHeader("Authorization") String auth) {
        return hrOrgService.deleteOrgAddress(orgId, addressId, auth);
    }
}

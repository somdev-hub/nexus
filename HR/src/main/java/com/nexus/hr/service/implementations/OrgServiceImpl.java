package com.nexus.hr.service.implementations;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.model.entities.OrgAccountInfo;
import com.nexus.hr.model.entities.OrgAddress;
import com.nexus.hr.repository.OrgAccountInfoRepo;
import com.nexus.hr.repository.OrgAddressRepo;
import com.nexus.hr.service.interfaces.OrgService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrgServiceImpl implements OrgService {

    private final OrgAccountInfoRepo orgAccountInfoRepo;

    private final OrgAddressRepo orgAddressRepo;

    @Override
    public ResponseEntity<?> createOrgAccountInfo(Long orgId, OrgAccountInfo orgAccountInfo, String auth) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(orgAccountInfo)) {
            return ResponseEntity.badRequest().body("Invalid input: Organization ID and account info are required");
        }
        ResponseEntity<?> response;
        try {
            orgAccountInfo.setOrgId(orgId);
            OrgAccountInfo savedInfo = orgAccountInfoRepo.save(orgAccountInfo);
            response = ResponseEntity.ok(savedInfo);
        } catch (Exception e) {
            response = ResponseEntity.internalServerError()
                    .body("An error occurred while saving organization account info: " + e.getMessage());
        }
        return response;

    }

    @Override
    public ResponseEntity<?> getOrgAccountInfo(Long orgId, String auth) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Invalid input: Organization ID is required");
        }
        OrgAccountInfo orgAccountInfo = orgAccountInfoRepo.findByOrgId(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("OrgAccountInfo", "OrgId", orgId));
        try {
            return ResponseEntity.ok(orgAccountInfo);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("An error occurred while fetching organization account info: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> updateOrgAccountInfo(Long orgId, OrgAccountInfo orgAccountInfo, String auth) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(orgAccountInfo)) {
            return ResponseEntity.badRequest().body("Invalid input: Organization ID and account info are required");
        }
        ResponseEntity<?> response;
        try {
            orgAccountInfo.setOrgId(orgId);
            OrgAccountInfo updatedInfo = orgAccountInfoRepo.save(orgAccountInfo);
            response = ResponseEntity.ok(updatedInfo);
        } catch (Exception e) {
            response = ResponseEntity.internalServerError()
                    .body("An error occurred while updating organization account info: " + e.getMessage());
        }
        return response;
    }

    @Override
    public ResponseEntity<?> deleteOrgAccountInfo(Long orgId, String auth) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Invalid input: Organization ID is required");
        }
        OrgAccountInfo orgAccountInfo = orgAccountInfoRepo.findByOrgId(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("OrgAccountInfo", "OrgId", orgId));
        try {
            orgAccountInfoRepo.delete(orgAccountInfo);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("An error occurred while deleting organization account info: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> createOrgAddress(Long orgId, OrgAddress orgAddress, String auth) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(orgAddress)) {
            return ResponseEntity.badRequest().body("Invalid input: Organization ID and address are required");
        }
        if (ObjectUtils.isEmpty(orgAddress.getAddressLine1()) || ObjectUtils.isEmpty(orgAddress.getCity())
                || ObjectUtils.isEmpty(orgAddress.getCountry())) {
            return ResponseEntity.badRequest()
                    .body("Invalid input: address line 1, city and country are required");
        }
        try {
            orgAddress.setOrgAddressId(null);
            orgAddress.setOrgId(orgId);
            clearConflictingDefaults(orgId, null, orgAddress);
            OrgAddress saved = orgAddressRepo.save(orgAddress);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("An error occurred while saving organization address: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getOrgAddresses(Long orgId, String auth) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Invalid input: Organization ID is required");
        }
        try {
            return ResponseEntity.ok(orgAddressRepo.findByOrgId(orgId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("An error occurred while fetching organization addresses: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getOrgAddress(Long orgId, Long addressId, String auth) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(addressId)) {
            return ResponseEntity.badRequest().body("Invalid input: Organization ID and address ID are required");
        }
        OrgAddress orgAddress = orgAddressRepo.findByOrgAddressIdAndOrgId(addressId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("OrgAddress", "orgAddressId", addressId));
        return ResponseEntity.ok(orgAddress);
    }

    @Override
    public ResponseEntity<?> updateOrgAddress(Long orgId, Long addressId, OrgAddress orgAddress, String auth) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(addressId) || ObjectUtils.isEmpty(orgAddress)) {
            return ResponseEntity.badRequest()
                    .body("Invalid input: Organization ID, address ID and address are required");
        }
        orgAddressRepo.findByOrgAddressIdAndOrgId(addressId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("OrgAddress", "orgAddressId", addressId));
        try {
            orgAddress.setOrgAddressId(addressId);
            orgAddress.setOrgId(orgId);
            clearConflictingDefaults(orgId, addressId, orgAddress);
            OrgAddress updated = orgAddressRepo.save(orgAddress);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("An error occurred while updating organization address: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> deleteOrgAddress(Long orgId, Long addressId, String auth) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(addressId)) {
            return ResponseEntity.badRequest().body("Invalid input: Organization ID and address ID are required");
        }
        OrgAddress orgAddress = orgAddressRepo.findByOrgAddressIdAndOrgId(addressId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("OrgAddress", "orgAddressId", addressId));
        try {
            orgAddressRepo.delete(orgAddress);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("An error occurred while deleting organization address: " + e.getMessage());
        }
    }

    /**
     * Keep at most one default billing / shipping address per org: when the
     * incoming address claims a default flag, unset it on every other
     * address of the same org.
     */
    private void clearConflictingDefaults(Long orgId, Long excludeAddressId, OrgAddress orgAddress) {
        if (Boolean.TRUE.equals(orgAddress.getIsDefaultShipping())) {
            for (OrgAddress other : orgAddressRepo.findByOrgIdAndIsDefaultShippingTrue(orgId)) {
                if (excludeAddressId == null || !excludeAddressId.equals(other.getOrgAddressId())) {
                    other.setIsDefaultShipping(false);
                    orgAddressRepo.save(other);
                }
            }
        }
        if (Boolean.TRUE.equals(orgAddress.getIsDefaultBilling())) {
            for (OrgAddress other : orgAddressRepo.findByOrgIdAndIsDefaultBillingTrue(orgId)) {
                if (excludeAddressId == null || !excludeAddressId.equals(other.getOrgAddressId())) {
                    other.setIsDefaultBilling(false);
                    orgAddressRepo.save(other);
                }
            }
        }
    }

}

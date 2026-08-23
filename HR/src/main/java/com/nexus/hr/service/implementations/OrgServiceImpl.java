package com.nexus.hr.service.implementations;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.model.entities.OrgAccountInfo;
import com.nexus.hr.repository.OrgAccountInfoRepo;
import com.nexus.hr.service.interfaces.OrgService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrgServiceImpl implements OrgService {

    private final OrgAccountInfoRepo orgAccountInfoRepo;

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
        ResponseEntity<?> response;
        try {
            OrgAccountInfo orgAccountInfo = orgAccountInfoRepo.findByOrgId(orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("OrgAccountInfo", "OrgId", orgId));
            if (ObjectUtils.isEmpty(orgAccountInfo)) {
                response = ResponseEntity.notFound().build();
            } else {
                response = ResponseEntity.ok(orgAccountInfo);
            }
        } catch (Exception e) {
            response = ResponseEntity.internalServerError()
                    .body("An error occurred while fetching organization account info: " + e.getMessage());
        }
        return response;
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

}

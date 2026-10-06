package com.nexus.iam.service;

import java.util.Map;

import org.springframework.http.ResponseEntity;

public interface HrOrgService {

    ResponseEntity<?> createOrgAccountInfo(Long orgId, Map<String, Object> dto, String authToken);

    ResponseEntity<?> getOrgAccountInfo(Long orgId, String authToken);

    ResponseEntity<?> updateOrgAccountInfo(Long orgId, Map<String, Object> dto, String authToken);

    ResponseEntity<?> deleteOrgAccountInfo(Long orgId, String authToken);

    ResponseEntity<?> createOrgAddress(Long orgId, Map<String, Object> dto, String authToken);

    ResponseEntity<?> getOrgAddresses(Long orgId, String authToken);

    ResponseEntity<?> getOrgAddress(Long orgId, Long addressId, String authToken);

    ResponseEntity<?> updateOrgAddress(Long orgId, Long addressId, Map<String, Object> dto, String authToken);

    ResponseEntity<?> deleteOrgAddress(Long orgId, Long addressId, String authToken);
}

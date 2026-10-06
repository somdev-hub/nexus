package com.nexus.hr.service.interfaces;

import org.springframework.http.ResponseEntity;

import com.nexus.hr.model.entities.OrgAccountInfo;
import com.nexus.hr.model.entities.OrgAddress;

public interface OrgService {

    public ResponseEntity<?> createOrgAccountInfo(Long orgId, OrgAccountInfo orgAccountInfo, String auth);

    public ResponseEntity<?> getOrgAccountInfo(Long orgId, String auth);

    public ResponseEntity<?> updateOrgAccountInfo(Long orgId, OrgAccountInfo orgAccountInfo, String auth);

    public ResponseEntity<?> deleteOrgAccountInfo(Long orgId, String auth);

    public ResponseEntity<?> createOrgAddress(Long orgId, OrgAddress orgAddress, String auth);

    public ResponseEntity<?> getOrgAddresses(Long orgId, String auth);

    public ResponseEntity<?> getOrgAddress(Long orgId, Long addressId, String auth);

    public ResponseEntity<?> updateOrgAddress(Long orgId, Long addressId, OrgAddress orgAddress, String auth);

    public ResponseEntity<?> deleteOrgAddress(Long orgId, Long addressId, String auth);
}

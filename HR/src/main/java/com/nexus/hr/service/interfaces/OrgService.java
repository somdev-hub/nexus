package com.nexus.hr.service.interfaces;

import org.springframework.http.ResponseEntity;

import com.nexus.hr.model.entities.OrgAccountInfo;

public interface OrgService {

    public ResponseEntity<?> createOrgAccountInfo(Long orgId, OrgAccountInfo orgAccountInfo, String auth);

    public ResponseEntity<?> getOrgAccountInfo(Long orgId, String auth);

    public ResponseEntity<?> updateOrgAccountInfo(Long orgId, OrgAccountInfo orgAccountInfo, String auth);
}

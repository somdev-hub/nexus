package com.nexus.core.service;

import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.MaterialDto;

public interface MaterialService {
    public ResponseEntity<?> addMaterial(MaterialDto materialDto, Long org);
}

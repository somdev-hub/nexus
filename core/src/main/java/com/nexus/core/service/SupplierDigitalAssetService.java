package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.SupplierDigitalAssetDto;

public interface SupplierDigitalAssetService {

    ResponseEntity<?> createAsset(SupplierDigitalAssetDto dto);

    ResponseEntity<?> getAsset(Long id);

    ResponseEntity<?> getAllAssets(Long catalogId, String assetType, Pageable pageable);

    ResponseEntity<?> updateAsset(Long id, SupplierDigitalAssetDto dto);

    ResponseEntity<?> deleteAsset(Long id);
}

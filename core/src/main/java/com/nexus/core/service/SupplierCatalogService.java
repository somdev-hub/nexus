package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.SupplierCatalogDto;

import java.util.Map;

public interface SupplierCatalogService {

    ResponseEntity<?> createCatalog(SupplierCatalogDto dto);

    ResponseEntity<?> getCatalog(Long id);

    ResponseEntity<?> getAllCatalogs(String status, String category, String family, String accessLevel, Boolean isPublished, String search, Pageable pageable);

    ResponseEntity<?> updateCatalog(Long id, SupplierCatalogDto dto);

    ResponseEntity<?> transitionStatus(Long id, String newStatus, Map<String, Object> params);

    ResponseEntity<?> deleteCatalog(Long id);

    ResponseEntity<?> getSummary();
}

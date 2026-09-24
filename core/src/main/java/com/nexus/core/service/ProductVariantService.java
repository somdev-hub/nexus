package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.ProductVariantDto;

public interface ProductVariantService {

    ResponseEntity<?> createVariant(ProductVariantDto dto);

    ResponseEntity<?> getVariant(Long id);

    ResponseEntity<?> getAllVariants(Long catalogId, String variantType, Long bomMaterialId, Pageable pageable);

    ResponseEntity<?> updateVariant(Long id, ProductVariantDto dto);

    ResponseEntity<?> deleteVariant(Long id);
}

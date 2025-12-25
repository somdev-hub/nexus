package com.nexus.core.service;

import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.ProductDto;

public interface ProductService {

    public ResponseEntity<?> addProduct(ProductDto product);

    public ResponseEntity<?> getProductById(Long id);

    public ResponseEntity<?> getAllProductsByOrgId(Long orgId);
}

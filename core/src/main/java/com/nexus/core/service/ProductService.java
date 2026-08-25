package com.nexus.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.ProductDto;

public interface ProductService {

	public ResponseEntity<?> addProduct(ProductDto product);

	public ResponseEntity<?> getProductById(Long id);

	public ResponseEntity<?> getProductByIdAndOrg(Long id, Long orgId);

	public ResponseEntity<?> getAllProductsByOrgId(Long orgId, Pageable pageable);
}

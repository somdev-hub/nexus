package com.nexus.core.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.core.entities.Product;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.ProductDto;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.ProductRepo;
import com.nexus.core.service.ProductService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ProductRepo productRepo;
	private final ModelMapper modelMapper;
	private final MaterialRepo materialRepo;

	@Override
	public ResponseEntity<?> addProduct(ProductDto product) {
		Product productMapped = modelMapper.map(product, Product.class);
		Product savedProduct = productRepo.save(productMapped);
		return new ResponseEntity<>(modelMapper.map(savedProduct, ProductDto.class), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getProductById(Long id) {
		Product product = productRepo.findByProductId(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", id));
		return new ResponseEntity<>(modelMapper.map(product, ProductDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getProductByIdAndOrg(Long id, Long orgId) {
		Product product = productRepo.findByProductIdAndOrg(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", id));
		return new ResponseEntity<>(modelMapper.map(product, ProductDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllProductsByOrgId(Long orgId, Pageable pageable) {
		Pageable safePageable = sanitizePageable(pageable);
		Page<Product> products = productRepo.findByOrg(orgId, safePageable);
		Page<ProductDto> productDtos = products.map(p -> modelMapper.map(p, ProductDto.class));
		return new ResponseEntity<>(productDtos, HttpStatus.OK);
	}

	private Pageable sanitizePageable(Pageable pageable) {
		if (pageable == null || pageable.isUnpaged()) {
			return pageable;
		}
		if (!pageable.getSort().isSorted()) {
			return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
		}
		for (Sort.Order order : pageable.getSort()) {
			if ("UNSORTED".equalsIgnoreCase(order.getProperty())) {
				return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
			}
		}
		return pageable;
	}

}

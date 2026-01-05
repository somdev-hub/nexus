package com.nexus.core.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.nexus.core.entities.Product;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.ErrorResponse;
import com.nexus.core.payload.ProductDto;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.ProductRepo;
import com.nexus.core.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private MaterialRepo materialRepo;

    @Override
    public ResponseEntity<?> addProduct(ProductDto product) {
        try {
            Product productMapped = modelMapper.map(product, Product.class);

            Product savedProduct = productRepo.save(productMapped);

            return new ResponseEntity<>(modelMapper.map(savedProduct, ProductDto.class), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<ErrorResponse>(
                    new ErrorResponse(
                            "Failed to add product",
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            Timestamp.valueOf(LocalDateTime.now()),
                            e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> getProductById(Long id) {
        if (ObjectUtils.isEmpty(id)) {
            return new ResponseEntity<ErrorResponse>(
                    new ErrorResponse(
                            "Product ID cannot be null or empty",
                            HttpStatus.BAD_REQUEST.value(),
                            Timestamp.valueOf(LocalDateTime.now()),
                            "Invalid Product ID"),
                    HttpStatus.BAD_REQUEST);

        }
        try {
            Product product = productRepo.findById(id).orElse(null);
            if (ObjectUtils.isEmpty(product)) {
                return new ResponseEntity<ErrorResponse>(
                        new ErrorResponse(
                                "Product not found",
                                HttpStatus.NOT_FOUND.value(),
                                Timestamp.valueOf(LocalDateTime.now()),
                                "No product found with the given ID"),
                        HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(modelMapper.map(product, ProductDto.class), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<ErrorResponse>(
                    new ErrorResponse(
                            "Failed to retrieve product",
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            Timestamp.valueOf(LocalDateTime.now()),
                            e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> getAllProductsByOrgId(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            return new ResponseEntity<ErrorResponse>(
                    new ErrorResponse(
                            "Organization ID cannot be null or empty",
                            HttpStatus.BAD_REQUEST.value(),
                            Timestamp.valueOf(LocalDateTime.now()),
                            "Invalid Organization ID"),
                    HttpStatus.BAD_REQUEST);

        }
        try {
            List<Product> products = productRepo.findByOrg(orgId).orElseThrow(() -> {
                throw new ResourceNotFoundException("Products", "orgId", orgId);
            });
            List<ProductDto> productDtos = new java.util.ArrayList<>();
            for (Product product : products) {
                productDtos.add(modelMapper.map(product, ProductDto.class));
            }
            return new ResponseEntity<>(productDtos, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<ErrorResponse>(
                    new ErrorResponse(
                            "Failed to retrieve products",
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            Timestamp.valueOf(LocalDateTime.now()),
                            e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

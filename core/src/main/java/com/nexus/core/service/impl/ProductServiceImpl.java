package com.nexus.core.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.core.entities.Product;
import com.nexus.core.payload.ErrorResponse;
import com.nexus.core.payload.ProductDto;
import com.nexus.core.repository.ProductRepo;
import com.nexus.core.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private ModelMapper modelMapper;

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

}

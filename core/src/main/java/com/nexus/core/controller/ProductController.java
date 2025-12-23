package com.nexus.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.exception.InvalidCredentialsException;
import com.nexus.core.payload.ProductDto;
import com.nexus.core.service.ProductService;
import com.nexus.core.utils.CommonUtils;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CommonUtils commonUtils;

    @PostMapping("/add")
    public ResponseEntity<?> addProduct(@RequestBody ProductDto product, @RequestHeader("Authorization") String token) {
        if (!commonUtils.validateToken(token)) {
            throw new InvalidCredentialsException();
        }
        try {
            return productService.addProduct(product);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e);
        }
    }

}

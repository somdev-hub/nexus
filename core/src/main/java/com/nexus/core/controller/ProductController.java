package com.nexus.core.controller;

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.entities.Logs;
import com.nexus.core.exception.InvalidCredentialsException;
import com.nexus.core.payload.ProductDto;
import com.nexus.core.service.ProductService;
import com.nexus.core.utils.CommonUtils;
import com.nexus.core.utils.Logger;

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

    @Autowired
    private Logger logger;

    @PostMapping("/add")
    public ResponseEntity<?> addProduct(@RequestBody ProductDto product, @RequestHeader("Authorization") String token) {
        if (!commonUtils.validateToken(token)) {
            throw new InvalidCredentialsException();
        }
        ResponseEntity<?> response = null;
        try {
            response = productService.addProduct(product);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e);
        } finally {
            logger.log("/iam/organization/add", HttpMethod.POST,
                    response != null ? response.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR, product,
                    response != null ? response.getBody() : null, product.getOrg());
        }
        return response;
    }

}

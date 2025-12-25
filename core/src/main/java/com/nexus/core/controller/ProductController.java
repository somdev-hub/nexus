package com.nexus.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.exception.InvalidCredentialsException;
import com.nexus.core.payload.ProductDto;
import com.nexus.core.service.ProductService;
import com.nexus.core.utils.CommonUtils;
import com.nexus.core.utils.Logger;

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
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } finally {
            logger.log("/products/add", HttpMethod.POST,
                    response != null ? response.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR, product,
                    response != null ? response.getBody() : null, product.getOrg());
        }
        return response;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@RequestParam Long id, @RequestHeader("Authorization") String token) {
        if (!commonUtils.validateToken(token)) {
            throw new InvalidCredentialsException();
        }
        ResponseEntity<?> response = null;
        try {
            response = productService.getProductById(id);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } finally {
            logger.log("/products/{id}", HttpMethod.GET,
                    response != null ? response.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR, id,
                    response != null ? response.getBody() : null, null);
        }

        return response;
    }

    @GetMapping("/all/{orgId}")
    public ResponseEntity<?> getAllProducts(@RequestParam Long orgId, @RequestHeader("Authorization") String token) {
        if (!commonUtils.validateToken(token)) {
            throw new InvalidCredentialsException();
        }
        ResponseEntity<?> response = null;
        try {
            response = productService.getAllProductsByOrgId(orgId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } finally {
            logger.log("/products/all/{orgId}", HttpMethod.GET,
                    response != null ? response.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR, orgId,
                    response != null ? response.getBody() : null, orgId);
        }
        return response;
    }

}

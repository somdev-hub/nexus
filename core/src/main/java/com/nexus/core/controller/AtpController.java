package com.nexus.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.service.AtpService;

import lombok.RequiredArgsConstructor;

import java.sql.Date;

@RestController
@RequestMapping("/core/supplier/atp")
@RequiredArgsConstructor
public class AtpController {

    private final AtpService atpService;

    @GetMapping("/catalog/{catalogId}")
    @LogActivity("Get ATP for Catalog")
    public ResponseEntity<?> getAtpForCatalog(@PathVariable Long catalogId,
            @RequestParam(required = false) Double requestedQuantity) {
        return atpService.getAvailableToPromise(catalogId, requestedQuantity);
    }

    @GetMapping("/product-line")
    @LogActivity("Get ATP for Product Line")
    public ResponseEntity<?> getAtpForProductLine(
            @RequestParam(required = false) String productLine,
            @RequestParam(required = false) Date date) {
        return atpService.getAtpForProductLine(productLine, date);
    }
}

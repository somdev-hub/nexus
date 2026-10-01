package com.nexus.core.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.service.interfaces.SupplierCatalogService;

import lombok.RequiredArgsConstructor;

/**
 * Cross-org catalog discovery for retailer browsing.
 * <p>
 * Exact contract consumed by gateway/UI:
 * {@code GET /core/catalog/browse?search=&category=&family=&supplierOrgId=&page=0&size=20}
 */
@RestController
@RequestMapping("/core/catalog")
@RequiredArgsConstructor
public class CatalogBrowseController {

    private final SupplierCatalogService catalogService;

    @GetMapping("/browse")
    @LogActivity("Browse Published Catalogs")
    public ResponseEntity<?> browseCatalogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String family,
            @RequestParam(required = false) Long supplierOrgId,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return catalogService.browseCatalogs(search, category, family, supplierOrgId, pageable);
    }
}

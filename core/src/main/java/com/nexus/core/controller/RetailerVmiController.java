package com.nexus.core.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.service.interfaces.VmiService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/retailer/vmi")
@RequiredArgsConstructor
public class RetailerVmiController {

    private final VmiService vmiService;

    @GetMapping("/all")
    @LogActivity("Get Retailer VMI Configs")
    public ResponseEntity<?> getAll(@RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long materialId,
            @PageableDefault(size = 20) Pageable pageable) {
        return vmiService.getRetailerVmis(warehouseId, materialId, pageable);
    }
}
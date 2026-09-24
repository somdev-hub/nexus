package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.VmiConfigDto;
import com.nexus.core.service.VmiService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/supplier/vmi")
@RequiredArgsConstructor
public class VmiController {

    private final VmiService vmiService;

    @PostMapping("/create")
    @LogActivity("Create VMI Config")
    public ResponseEntity<?> createVmi(@Valid @RequestBody VmiConfigDto dto) {
        return vmiService.createVmi(dto);
    }

    @GetMapping("/{id}")
    @LogActivity("Get VMI Config")
    public ResponseEntity<?> getVmi(@PathVariable Long id) {
        return vmiService.getVmi(id);
    }

    @GetMapping("/all")
    @LogActivity("Get All VMI Configs")
    public ResponseEntity<?> getAllVmis(
            @RequestParam(required = false) Long retailerOrgId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Boolean autoReplenish,
            @PageableDefault(size = 20) Pageable pageable) {
        return vmiService.getAllVmis(retailerOrgId, warehouseId, materialId, autoReplenish, pageable);
    }

    @PutMapping("/{id}/update")
    @LogActivity("Update VMI Config")
    public ResponseEntity<?> updateVmi(@PathVariable Long id, @RequestBody VmiConfigDto dto) {
        return vmiService.updateVmi(id, dto);
    }

    @DeleteMapping("/{id}")
    @LogActivity("Delete VMI Config")
    public ResponseEntity<?> deleteVmi(@PathVariable Long id) {
        return vmiService.deleteVmi(id);
    }

    @PostMapping("/{id}/replenish")
    @LogActivity("Trigger VMI Replenishment")
    public ResponseEntity<?> triggerReplenishment(@PathVariable Long id) {
        return vmiService.triggerReplenishment(id);
    }

    @GetMapping("/replenishment-suggestions")
    @LogActivity("Get VMI Replenishment Suggestions")
    public ResponseEntity<?> getSuggestions() {
        return vmiService.getReplenishmentSuggestions();
    }
}

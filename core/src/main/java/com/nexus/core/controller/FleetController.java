package com.nexus.core.controller;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.DriverDto;
import com.nexus.core.payload.FleetAssetDto;
import com.nexus.core.payload.MaintenanceRecordDto;
import com.nexus.core.service.FleetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/core/logistics/fleet")
@RequiredArgsConstructor
public class FleetController {

    private final FleetService fleetService;

    @PostMapping("/assets/create")
    @LogActivity("Create Fleet Asset")
    public ResponseEntity<?> createAsset(@Valid @RequestBody FleetAssetDto dto) {
        return fleetService.createAsset(dto);
    }

    @GetMapping("/assets/{id}")
    @LogActivity("Get Fleet Asset")
    public ResponseEntity<?> getAsset(@PathVariable Long id) {
        return fleetService.getAsset(id);
    }

    @GetMapping("/assets/all")
    @LogActivity("Get All Fleet Assets")
    public ResponseEntity<?> getAllAssets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return fleetService.getAllAssets(status, assetType, search, pageable);
    }

    @PutMapping("/assets/{id}/update")
    @LogActivity("Update Fleet Asset")
    public ResponseEntity<?> updateAsset(@PathVariable Long id, @RequestBody FleetAssetDto dto) {
        return fleetService.updateAsset(id, dto);
    }

    @PutMapping("/assets/{id}/status")
    @LogActivity("Transition Fleet Asset Status")
    public ResponseEntity<?> transitionAssetStatus(@PathVariable Long id,
            @RequestParam String newStatus,
            @RequestBody(required = false) Map<String, Object> params) {
        return fleetService.transitionAssetStatus(id, newStatus, params);
    }

    @DeleteMapping("/assets/{id}")
    @LogActivity("Delete Fleet Asset")
    public ResponseEntity<?> deleteAsset(@PathVariable Long id) {
        return fleetService.deleteAsset(id);
    }

    @GetMapping("/assets/summary")
    @LogActivity("Get Fleet Asset Summary")
    public ResponseEntity<?> getAssetSummary() {
        return fleetService.getAssetSummary();
    }

    @GetMapping("/assets/{id}/shipments")
    @LogActivity("Get Asset Shipment History")
    public ResponseEntity<?> getAssetShipments(@PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @PageableDefault(size = 20) Pageable pageable) {
        return fleetService.getAssetShipments(id, status, from, to, pageable);
    }

    @GetMapping("/assets/{id}/drivers")
    @LogActivity("Get Asset Driver History")
    public ResponseEntity<?> getAssetDrivers(@PathVariable Long id) {
        return fleetService.getAssetDrivers(id);
    }

    @GetMapping("/assets/{id}/current-shipment")
    @LogActivity("Get Asset Current Shipment")
    public ResponseEntity<?> getAssetCurrentShipment(@PathVariable Long id) {
        return fleetService.getAssetCurrentShipment(id);
    }

    @PostMapping("/drivers/create")
    @LogActivity("Create Driver")
    public ResponseEntity<?> createDriver(@Valid @RequestBody DriverDto dto) {
        return fleetService.createDriver(dto);
    }

    @GetMapping("/drivers/{id}")
    @LogActivity("Get Driver")
    public ResponseEntity<?> getDriver(@PathVariable Long id) {
        return fleetService.getDriver(id);
    }

    @GetMapping("/drivers/all")
    @LogActivity("Get All Drivers")
    public ResponseEntity<?> getAllDrivers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return fleetService.getAllDrivers(status, search, pageable);
    }

    @PutMapping("/drivers/{id}/update")
    @LogActivity("Update Driver")
    public ResponseEntity<?> updateDriver(@PathVariable Long id, @RequestBody DriverDto dto) {
        return fleetService.updateDriver(id, dto);
    }

    @PutMapping("/drivers/{id}/status")
    @LogActivity("Transition Driver Status")
    public ResponseEntity<?> transitionDriverStatus(@PathVariable Long id,
            @RequestParam String newStatus,
            @RequestBody(required = false) Map<String, Object> params) {
        return fleetService.transitionDriverStatus(id, newStatus, params);
    }

    @DeleteMapping("/drivers/{id}")
    @LogActivity("Delete Driver")
    public ResponseEntity<?> deleteDriver(@PathVariable Long id) {
        return fleetService.deleteDriver(id);
    }

    @PostMapping("/maintenance/create")
    @LogActivity("Create Maintenance Record")
    public ResponseEntity<?> createMaintenance(@Valid @RequestBody MaintenanceRecordDto dto) {
        return fleetService.createMaintenance(dto);
    }

    @GetMapping("/maintenance/{id}")
    @LogActivity("Get Maintenance Record")
    public ResponseEntity<?> getMaintenance(@PathVariable Long id) {
        return fleetService.getMaintenance(id);
    }

    @PutMapping("/maintenance/{id}/update")
    @LogActivity("Update Maintenance Record")
    public ResponseEntity<?> updateMaintenance(@PathVariable Long id, @RequestBody MaintenanceRecordDto dto) {
        return fleetService.updateMaintenance(id, dto);
    }

    @GetMapping("/maintenance/all")
    @LogActivity("Get All Maintenance Records")
    public ResponseEntity<?> getAllMaintenance(
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isBreakdown,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return fleetService.getAllMaintenance(assetId, status, isBreakdown, search, pageable);
    }

    @PutMapping("/maintenance/{id}/status")
    @LogActivity("Transition Maintenance Status")
    public ResponseEntity<?> transitionMaintenanceStatus(@PathVariable Long id,
            @RequestParam String newStatus,
            @RequestBody(required = false) Map<String, Object> params) {
        return fleetService.transitionMaintenanceStatus(id, newStatus, params);
    }

    @DeleteMapping("/maintenance/{id}")
    @LogActivity("Delete Maintenance Record")
    public ResponseEntity<?> deleteMaintenance(@PathVariable Long id) {
        return fleetService.deleteMaintenance(id);
    }
}

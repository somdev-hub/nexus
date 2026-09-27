package com.nexus.core.service;

import com.nexus.core.payload.DriverDto;
import com.nexus.core.payload.FleetAssetDto;
import com.nexus.core.payload.MaintenanceRecordDto;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface FleetService {

    // Assets
    ResponseEntity<?> createAsset(FleetAssetDto dto);
    ResponseEntity<?> getAsset(Long id);
    ResponseEntity<?> getAllAssets(String status, String assetType, String search, Pageable pageable);
    ResponseEntity<?> updateAsset(Long id, FleetAssetDto dto);
    ResponseEntity<?> transitionAssetStatus(Long id, String newStatus, Map<String, Object> params);
    ResponseEntity<?> deleteAsset(Long id);
    ResponseEntity<?> getAssetSummary();

    // Drivers
    ResponseEntity<?> createDriver(DriverDto dto);
    ResponseEntity<?> getDriver(Long id);
    ResponseEntity<?> getAllDrivers(String status, String search, Pageable pageable);
    ResponseEntity<?> updateDriver(Long id, DriverDto dto);
    ResponseEntity<?> transitionDriverStatus(Long id, String newStatus, Map<String, Object> params);
    ResponseEntity<?> deleteDriver(Long id);

    // Maintenance
    ResponseEntity<?> createMaintenance(MaintenanceRecordDto dto);
    ResponseEntity<?> getMaintenance(Long id);
    ResponseEntity<?> getAllMaintenance(Long assetId, String status, Boolean isBreakdown, String search, Pageable pageable);
    ResponseEntity<?> transitionMaintenanceStatus(Long id, String newStatus, Map<String, Object> params);
    ResponseEntity<?> deleteMaintenance(Long id);
}

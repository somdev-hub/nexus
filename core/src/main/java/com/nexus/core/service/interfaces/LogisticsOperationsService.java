package com.nexus.core.service.interfaces;

import com.nexus.core.payload.CapacityForecastDto;
import com.nexus.core.payload.CarrierPayableDto;
import com.nexus.core.payload.ConsolidationGroupDto;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface LogisticsOperationsService {

    // Consolidation (FR-LOG-005)
    ResponseEntity<?> createGroup(ConsolidationGroupDto dto);
    ResponseEntity<?> getGroup(Long id);
    ResponseEntity<?> getAllGroups(String status, String search, Pageable pageable);
    ResponseEntity<?> updateGroup(Long id, ConsolidationGroupDto dto);
    ResponseEntity<?> transitionGroupStatus(Long id, String newStatus, Map<String, Object> params);
    ResponseEntity<?> addShipmentsToGroup(Long id, Map<String, Object> body);
    ResponseEntity<?> deleteGroup(Long id);

    // Capacity (FR-LOG-014)
    ResponseEntity<?> createCapacity(CapacityForecastDto dto);
    ResponseEntity<?> getCapacity(Long id);
    ResponseEntity<?> getAllCapacities(String equipmentType, String search, Pageable pageable);
    ResponseEntity<?> updateCapacity(Long id, CapacityForecastDto dto);
    ResponseEntity<?> deleteCapacity(Long id);

    // Carrier payables (FR-LOG-032, settlement execution delegated to PMS)
    ResponseEntity<?> createPayable(CarrierPayableDto dto);
    ResponseEntity<?> getPayable(Long id);
    ResponseEntity<?> getAllPayables(Long shipmentId, String status, String search, Pageable pageable);
    ResponseEntity<?> updatePayable(Long id, CarrierPayableDto dto);
    ResponseEntity<?> transitionPayableStatus(Long id, String newStatus, Map<String, Object> params);
    ResponseEntity<?> deletePayable(Long id);

    // Analytics (FR-LOG-033)
    ResponseEntity<?> getLogisticsDashboard();
}

package com.nexus.core.controller;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.CapacityForecastDto;
import com.nexus.core.payload.CarrierPayableDto;
import com.nexus.core.payload.ConsolidationGroupDto;
import com.nexus.core.service.interfaces.LogisticsOperationsService;
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
@RequestMapping("/core/logistics/operations")
@RequiredArgsConstructor
public class LogisticsOperationsController {

    private final LogisticsOperationsService operationsService;

    @PostMapping("/consolidation/create")
    @LogActivity("Create Consolidation Group")
    public ResponseEntity<?> createGroup(@Valid @RequestBody ConsolidationGroupDto dto) {
        return operationsService.createGroup(dto);
    }

    @GetMapping("/consolidation/{id}")
    @LogActivity("Get Consolidation Group")
    public ResponseEntity<?> getGroup(@PathVariable Long id) {
        return operationsService.getGroup(id);
    }

    @GetMapping("/consolidation/all")
    @LogActivity("Get All Consolidation Groups")
    public ResponseEntity<?> getAllGroups(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return operationsService.getAllGroups(status, search, pageable);
    }

    @PutMapping("/consolidation/{id}/update")
    @LogActivity("Update Consolidation Group")
    public ResponseEntity<?> updateGroup(@PathVariable Long id, @RequestBody ConsolidationGroupDto dto) {
        return operationsService.updateGroup(id, dto);
    }

    @PutMapping("/consolidation/{id}/status")
    @LogActivity("Transition Consolidation Group Status")
    public ResponseEntity<?> transitionGroupStatus(@PathVariable Long id,
            @RequestParam String newStatus,
            @RequestBody(required = false) Map<String, Object> params) {
        return operationsService.transitionGroupStatus(id, newStatus, params);
    }

    @PostMapping("/consolidation/{id}/shipments")
    @LogActivity("Add Shipments to Consolidation Group")
    public ResponseEntity<?> addShipmentsToGroup(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return operationsService.addShipmentsToGroup(id, body);
    }

    @DeleteMapping("/consolidation/{id}")
    @LogActivity("Delete Consolidation Group")
    public ResponseEntity<?> deleteGroup(@PathVariable Long id) {
        return operationsService.deleteGroup(id);
    }

    @PostMapping("/capacity/create")
    @LogActivity("Create Capacity Forecast")
    public ResponseEntity<?> createCapacity(@Valid @RequestBody CapacityForecastDto dto) {
        return operationsService.createCapacity(dto);
    }

    @GetMapping("/capacity/{id}")
    @LogActivity("Get Capacity Forecast")
    public ResponseEntity<?> getCapacity(@PathVariable Long id) {
        return operationsService.getCapacity(id);
    }

    @GetMapping("/capacity/all")
    @LogActivity("Get All Capacity Forecasts")
    public ResponseEntity<?> getAllCapacities(
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return operationsService.getAllCapacities(equipmentType, search, pageable);
    }

    @PutMapping("/capacity/{id}/update")
    @LogActivity("Update Capacity Forecast")
    public ResponseEntity<?> updateCapacity(@PathVariable Long id, @RequestBody CapacityForecastDto dto) {
        return operationsService.updateCapacity(id, dto);
    }

    @DeleteMapping("/capacity/{id}")
    @LogActivity("Delete Capacity Forecast")
    public ResponseEntity<?> deleteCapacity(@PathVariable Long id) {
        return operationsService.deleteCapacity(id);
    }

    @PutMapping("/capacity/{id}/extend-for-partnership")
    @LogActivity("Extend Capacity For Long-Term Partnership")
    public ResponseEntity<?> extendCapacityForPartnership(@PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return operationsService.extendCapacityForPartnership(id, body);
    }

    @PostMapping("/payables/create")
    @LogActivity("Create Carrier Payable")
    public ResponseEntity<?> createPayable(@Valid @RequestBody CarrierPayableDto dto) {
        return operationsService.createPayable(dto);
    }

    @PutMapping("/payables/{id}/update")
    @LogActivity("Update Carrier Payable")
    public ResponseEntity<?> updatePayable(@PathVariable Long id, @RequestBody CarrierPayableDto dto) {
        return operationsService.updatePayable(id, dto);
    }

    @GetMapping("/payables/{id}")
    @LogActivity("Get Carrier Payable")
    public ResponseEntity<?> getPayable(@PathVariable Long id) {
        return operationsService.getPayable(id);
    }

    @GetMapping("/payables/all")
    @LogActivity("Get All Carrier Payables")
    public ResponseEntity<?> getAllPayables(
            @RequestParam(required = false) Long shipmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return operationsService.getAllPayables(shipmentId, status, search, pageable);
    }

    @PutMapping("/payables/{id}/status")
    @LogActivity("Transition Carrier Payable Status")
    public ResponseEntity<?> transitionPayableStatus(@PathVariable Long id,
            @RequestParam String newStatus,
            @RequestBody(required = false) Map<String, Object> params) {
        return operationsService.transitionPayableStatus(id, newStatus, params);
    }

    @DeleteMapping("/payables/{id}")
    @LogActivity("Delete Carrier Payable")
    public ResponseEntity<?> deletePayable(@PathVariable Long id) {
        return operationsService.deletePayable(id);
    }

    @GetMapping("/analytics/dashboard")
    @LogActivity("Get Logistics Dashboard")
    public ResponseEntity<?> getLogisticsDashboard() {
        return operationsService.getLogisticsDashboard();
    }
}

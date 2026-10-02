package com.nexus.iam.controller;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.service.CoreLogisticsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core Logistics Controller
 * <p>
 * Handles logistics-specific Core module APIs through IAM gateway.
 * Frontend calls these endpoints for logistics operations.
 */
@Slf4j
@RestController
@RequestMapping("/iam/core/logistics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CoreLogisticsController {

	private final CoreLogisticsService logisticsService;

	// Fleet assets
	@LogActivity("Create Fleet Asset via IAM")
	@PostMapping("/fleet/assets/create")
	public ResponseEntity<?> createAsset(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.createAsset(dto, auth, org);
	}

	@LogActivity("Get Fleet Asset via IAM")
	@GetMapping("/fleet/assets/{id}")
	public ResponseEntity<?> getAsset(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.getAsset(id, auth, org);
	}

	@LogActivity("Get All Fleet Assets via IAM")
	@GetMapping("/fleet/assets/all")
	public ResponseEntity<?> getAllAssets(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) String status, @RequestParam(required = false) String assetType,
			@RequestParam(required = false) String search) {
		return logisticsService.getAllAssets(auth, org, p, status, assetType, search);
	}

	@LogActivity("Update Fleet Asset via IAM")
	@PutMapping("/fleet/assets/{id}/update")
	public ResponseEntity<?> updateAsset(@PathVariable Long id, @RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.updateAsset(id, dto, auth, org);
	}

	@LogActivity("Transition Fleet Asset Status via IAM")
	@PutMapping("/fleet/assets/{id}/status")
	public ResponseEntity<?> transitionAssetStatus(@PathVariable Long id, @RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.transitionAssetStatus(id, newStatus, params, auth, org);
	}

	@LogActivity("Delete Fleet Asset via IAM")
	@DeleteMapping("/fleet/assets/{id}")
	public ResponseEntity<?> deleteAsset(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.deleteAsset(id, auth, org);
	}

    @LogActivity("Get Fleet Asset Summary via IAM")
    @GetMapping("/fleet/assets/summary")
    public ResponseEntity<?> getAssetSummary(@RequestHeader("Authorization") String auth,
            @RequestHeader("X-Organization-ID") String org) {
        return logisticsService.getAssetSummary(auth, org);
    }

    @LogActivity("Get Asset Shipments via IAM")
    @GetMapping("/fleet/assets/{id}/shipments")
    public ResponseEntity<?> getAssetShipments(@PathVariable Long id, @RequestHeader("Authorization") String auth,
            @RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
            @RequestParam(required = false) String status, @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return logisticsService.getAssetShipments(id, auth, org, p, status, from, to);
    }

    @LogActivity("Get Asset Drivers via IAM")
    @GetMapping("/fleet/assets/{id}/drivers")
    public ResponseEntity<?> getAssetDrivers(@PathVariable Long id, @RequestHeader("Authorization") String auth,
            @RequestHeader("X-Organization-ID") String org) {
        return logisticsService.getAssetDrivers(id, auth, org);
    }

    @LogActivity("Get Asset Current Shipment via IAM")
    @GetMapping("/fleet/assets/{id}/current-shipment")
    public ResponseEntity<?> getAssetCurrentShipment(@PathVariable Long id, @RequestHeader("Authorization") String auth,
            @RequestHeader("X-Organization-ID") String org) {
        return logisticsService.getAssetCurrentShipment(id, auth, org);
    }

	// Drivers
	@LogActivity("Create Driver via IAM")
	@PostMapping("/fleet/drivers/create")
	public ResponseEntity<?> createDriver(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.createDriver(dto, auth, org);
	}

	@LogActivity("Get Driver via IAM")
	@GetMapping("/fleet/drivers/{id}")
	public ResponseEntity<?> getDriver(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.getDriver(id, auth, org);
	}

	@LogActivity("Get All Drivers via IAM")
	@GetMapping("/fleet/drivers/all")
	public ResponseEntity<?> getAllDrivers(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) String status, @RequestParam(required = false) String search) {
		return logisticsService.getAllDrivers(auth, org, p, status, search);
	}

	@LogActivity("Update Driver via IAM")
	@PutMapping("/fleet/drivers/{id}/update")
	public ResponseEntity<?> updateDriver(@PathVariable Long id, @RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.updateDriver(id, dto, auth, org);
	}

	@LogActivity("Transition Driver Status via IAM")
	@PutMapping("/fleet/drivers/{id}/status")
	public ResponseEntity<?> transitionDriverStatus(@PathVariable Long id, @RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.transitionDriverStatus(id, newStatus, params, auth, org);
	}

	@LogActivity("Delete Driver via IAM")
	@DeleteMapping("/fleet/drivers/{id}")
	public ResponseEntity<?> deleteDriver(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.deleteDriver(id, auth, org);
	}

	// Maintenance
	@LogActivity("Create Maintenance via IAM")
	@PostMapping("/fleet/maintenance/create")
	public ResponseEntity<?> createMaintenance(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.createMaintenance(dto, auth, org);
	}

	@LogActivity("Get Maintenance via IAM")
	@GetMapping("/fleet/maintenance/{id}")
	public ResponseEntity<?> getMaintenance(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.getMaintenance(id, auth, org);
	}

	@LogActivity("Get All Maintenance via IAM")
	@GetMapping("/fleet/maintenance/all")
	public ResponseEntity<?> getAllMaintenance(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) Long assetId, @RequestParam(required = false) String status,
			@RequestParam(required = false) Boolean isBreakdown, @RequestParam(required = false) String search) {
		return logisticsService.getAllMaintenance(auth, org, p, assetId, status, isBreakdown, search);
	}

    @LogActivity("Update Maintenance via IAM")
    @PutMapping("/fleet/maintenance/{id}/update")
    public ResponseEntity<?> updateMaintenance(@PathVariable Long id, @RequestBody Map<String, Object> dto,
            @RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
        return logisticsService.updateMaintenance(id, dto, auth, org);
    }

    @LogActivity("Transition Maintenance Status via IAM")
	@PutMapping("/fleet/maintenance/{id}/status")
	public ResponseEntity<?> transitionMaintenanceStatus(@PathVariable Long id, @RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.transitionMaintenanceStatus(id, newStatus, params, auth, org);
	}

	@LogActivity("Delete Maintenance via IAM")
	@DeleteMapping("/fleet/maintenance/{id}")
	public ResponseEntity<?> deleteMaintenance(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.deleteMaintenance(id, auth, org);
	}

	// Load board + quotes + rates
	@LogActivity("Get Load Board via IAM")
	@GetMapping("/quoting/load-board")
	public ResponseEntity<?> getLoadBoard(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) String mode, @RequestParam(required = false) String search) {
		return logisticsService.getLoadBoard(auth, org, p, mode, search);
	}

	@LogActivity("Create Shipment Quote via IAM")
	@PostMapping("/quoting/quotes/create")
	public ResponseEntity<?> createQuote(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.createQuote(dto, auth, org);
	}

	@LogActivity("Get Shipment Quote via IAM")
	@GetMapping("/quoting/quotes/{id}")
	public ResponseEntity<?> getQuote(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.getQuote(id, auth, org);
	}

	@LogActivity("Get All Shipment Quotes via IAM")
	@GetMapping("/quoting/quotes/all")
	public ResponseEntity<?> getAllQuotes(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) Long shipmentId, @RequestParam(required = false) String status,
			@RequestParam(required = false) String search) {
		return logisticsService.getAllQuotes(auth, org, p, shipmentId, status, search);
	}

	@LogActivity("Update Shipment Quote via IAM")
	@PutMapping("/quoting/quotes/{id}/update")
	public ResponseEntity<?> updateQuote(@PathVariable Long id, @RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.updateQuote(id, dto, auth, org);
	}

	@LogActivity("Transition Shipment Quote Status via IAM")
	@PutMapping("/quoting/quotes/{id}/status")
	public ResponseEntity<?> transitionQuoteStatus(@PathVariable Long id, @RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.transitionQuoteStatus(id, newStatus, params, auth, org);
	}

	@LogActivity("Delete Shipment Quote via IAM")
	@DeleteMapping("/quoting/quotes/{id}")
	public ResponseEntity<?> deleteQuote(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.deleteQuote(id, auth, org);
	}

	@LogActivity("Create Freight Rate via IAM")
	@PostMapping("/quoting/rates/create")
	public ResponseEntity<?> createRate(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.createRate(dto, auth, org);
	}

	@LogActivity("Get Freight Rate via IAM")
	@GetMapping("/quoting/rates/{id}")
	public ResponseEntity<?> getRate(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.getRate(id, auth, org);
	}

	@LogActivity("Get All Freight Rates via IAM")
	@GetMapping("/quoting/rates/all")
	public ResponseEntity<?> getAllRates(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) String rateType, @RequestParam(required = false) String equipmentType,
			@RequestParam(required = false) String search) {
		return logisticsService.getAllRates(auth, org, p, rateType, equipmentType, search);
	}

	@LogActivity("Update Freight Rate via IAM")
	@PutMapping("/quoting/rates/{id}/update")
	public ResponseEntity<?> updateRate(@PathVariable Long id, @RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.updateRate(id, dto, auth, org);
	}

	@LogActivity("Delete Freight Rate via IAM")
	@DeleteMapping("/quoting/rates/{id}")
	public ResponseEntity<?> deleteRate(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.deleteRate(id, auth, org);
	}

	// Execution
	@LogActivity("Assign Logistics Booking via IAM")
	@PutMapping("/execution/shipments/{shipmentId}/assign")
	public ResponseEntity<?> assignBooking(@PathVariable Long shipmentId,
			@RequestBody(required = false) Map<String, Object> body, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.assignBooking(shipmentId, body, auth, org);
	}

    @LogActivity("Transition Logistics Shipment Status via IAM")
    @PutMapping("/execution/shipments/{shipmentId}/status")
    public ResponseEntity<?> transitionShipmentStatus(@PathVariable Long shipmentId, @RequestParam String newStatus,
            @RequestBody(required = false) Map<String, Object> params, @RequestHeader("Authorization") String auth,
            @RequestHeader("X-Organization-ID") String org) {
        return logisticsService.transitionShipmentStatus(shipmentId, newStatus, params, auth, org);
    }

    @LogActivity("Get Shipment Live Position via IAM")
    @GetMapping("/execution/shipments/{shipmentId}/position")
    public ResponseEntity<?> getShipmentPosition(@PathVariable Long shipmentId,
            @RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
        return logisticsService.getShipmentPosition(shipmentId, auth, org);
    }

    @LogActivity("Update Shipment Route via IAM")
    @PutMapping("/execution/shipments/{shipmentId}/route")
    public ResponseEntity<?> updateShipmentRoute(@PathVariable Long shipmentId,
            @RequestBody(required = false) Map<String, Object> body, @RequestHeader("Authorization") String auth,
            @RequestHeader("X-Organization-ID") String org) {
        return logisticsService.updateShipmentRoute(shipmentId, body, auth, org);
    }

    @LogActivity("Get Shipment ETA via IAM")
	@GetMapping("/execution/shipments/{shipmentId}/eta")
	public ResponseEntity<?> getShipmentEta(@PathVariable Long shipmentId,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.getShipmentEta(shipmentId, auth, org);
	}

	@LogActivity("Capture POD via IAM")
	@PostMapping("/execution/pod/capture")
	public ResponseEntity<?> capturePod(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.capturePod(dto, auth, org);
	}

    @LogActivity("Update POD via IAM")
    @PutMapping("/execution/pod/{id}/update")
    public ResponseEntity<?> updatePod(@PathVariable Long id, @RequestBody Map<String, Object> dto,
            @RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
        return logisticsService.updatePod(id, dto, auth, org);
    }

    @LogActivity("Get POD by Shipment via IAM")
	@GetMapping("/execution/pod/shipment/{shipmentId}")
	public ResponseEntity<?> getPodByShipment(@PathVariable Long shipmentId,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.getPodByShipment(shipmentId, auth, org);
	}

	@LogActivity("Get All PODs via IAM")
	@GetMapping("/execution/pod/all")
	public ResponseEntity<?> getAllPods(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) String search) {
		return logisticsService.getAllPods(auth, org, p, search);
	}

	@LogActivity("Report Shipment Incident via IAM")
	@PostMapping("/execution/incidents/report")
	public ResponseEntity<?> reportIncident(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.reportIncident(dto, auth, org);
	}

    @LogActivity("Update Shipment Incident via IAM")
    @PutMapping("/execution/incidents/{id}/update")
    public ResponseEntity<?> updateIncident(@PathVariable Long id, @RequestBody Map<String, Object> dto,
            @RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
        return logisticsService.updateIncident(id, dto, auth, org);
    }

    @LogActivity("Get Shipment Incident via IAM")
	@GetMapping("/execution/incidents/{id}")
	public ResponseEntity<?> getIncident(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.getIncident(id, auth, org);
	}

	@LogActivity("Get All Shipment Incidents via IAM")
	@GetMapping("/execution/incidents/all")
	public ResponseEntity<?> getAllIncidents(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) Long shipmentId, @RequestParam(required = false) String incidentType,
			@RequestParam(required = false) String status, @RequestParam(required = false) String search) {
		return logisticsService.getAllIncidents(auth, org, p, shipmentId, incidentType, status, search);
	}

	@LogActivity("Transition Shipment Incident Status via IAM")
	@PutMapping("/execution/incidents/{id}/status")
	public ResponseEntity<?> transitionIncidentStatus(@PathVariable Long id, @RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.transitionIncidentStatus(id, newStatus, params, auth, org);
	}

	// Operations
	@LogActivity("Create Consolidation Group via IAM")
	@PostMapping("/operations/consolidation/create")
	public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.createGroup(dto, auth, org);
	}

	@LogActivity("Get Consolidation Group via IAM")
	@GetMapping("/operations/consolidation/{id}")
	public ResponseEntity<?> getGroup(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.getGroup(id, auth, org);
	}

	@LogActivity("Get All Consolidation Groups via IAM")
	@GetMapping("/operations/consolidation/all")
	public ResponseEntity<?> getAllGroups(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) String status, @RequestParam(required = false) String search) {
		return logisticsService.getAllGroups(auth, org, p, status, search);
	}

	@LogActivity("Update Consolidation Group via IAM")
	@PutMapping("/operations/consolidation/{id}/update")
	public ResponseEntity<?> updateGroup(@PathVariable Long id, @RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.updateGroup(id, dto, auth, org);
	}

	@LogActivity("Transition Consolidation Group Status via IAM")
	@PutMapping("/operations/consolidation/{id}/status")
	public ResponseEntity<?> transitionGroupStatus(@PathVariable Long id, @RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.transitionGroupStatus(id, newStatus, params, auth, org);
	}

	@LogActivity("Add Shipments to Consolidation Group via IAM")
	@PostMapping("/operations/consolidation/{id}/shipments")
	public ResponseEntity<?> addShipmentsToGroup(@PathVariable Long id, @RequestBody Map<String, Object> body,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.addShipmentsToGroup(id, body, auth, org);
	}

	@LogActivity("Delete Consolidation Group via IAM")
	@DeleteMapping("/operations/consolidation/{id}")
	public ResponseEntity<?> deleteGroup(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.deleteGroup(id, auth, org);
	}

	@LogActivity("Create Capacity Forecast via IAM")
	@PostMapping("/operations/capacity/create")
	public ResponseEntity<?> createCapacity(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.createCapacity(dto, auth, org);
	}

	@LogActivity("Get Capacity Forecast via IAM")
	@GetMapping("/operations/capacity/{id}")
	public ResponseEntity<?> getCapacity(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.getCapacity(id, auth, org);
	}

	@LogActivity("Get All Capacity Forecasts via IAM")
	@GetMapping("/operations/capacity/all")
	public ResponseEntity<?> getAllCapacities(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) String equipmentType, @RequestParam(required = false) String search) {
		return logisticsService.getAllCapacities(auth, org, p, equipmentType, search);
	}

	@LogActivity("Update Capacity Forecast via IAM")
	@PutMapping("/operations/capacity/{id}/update")
	public ResponseEntity<?> updateCapacity(@PathVariable Long id, @RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.updateCapacity(id, dto, auth, org);
	}

	@LogActivity("Delete Capacity Forecast via IAM")
	@DeleteMapping("/operations/capacity/{id}")
	public ResponseEntity<?> deleteCapacity(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.deleteCapacity(id, auth, org);
	}

	@LogActivity("Create Carrier Payable via IAM")
	@PostMapping("/operations/payables/create")
	public ResponseEntity<?> createPayable(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.createPayable(dto, auth, org);
	}

    @LogActivity("Update Carrier Payable via IAM")
    @PutMapping("/operations/payables/{id}/update")
    public ResponseEntity<?> updatePayable(@PathVariable Long id, @RequestBody Map<String, Object> dto,
            @RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
        return logisticsService.updatePayable(id, dto, auth, org);
    }

    @LogActivity("Get Carrier Payable via IAM")
	@GetMapping("/operations/payables/{id}")
	public ResponseEntity<?> getPayable(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.getPayable(id, auth, org);
	}

	@LogActivity("Get All Carrier Payables via IAM")
	@GetMapping("/operations/payables/all")
	public ResponseEntity<?> getAllPayables(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) Long shipmentId, @RequestParam(required = false) String status,
			@RequestParam(required = false) String search) {
		return logisticsService.getAllPayables(auth, org, p, shipmentId, status, search);
	}

	@LogActivity("Transition Carrier Payable Status via IAM")
	@PutMapping("/operations/payables/{id}/status")
	public ResponseEntity<?> transitionPayableStatus(@PathVariable Long id, @RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.transitionPayableStatus(id, newStatus, params, auth, org);
	}

	@LogActivity("Delete Carrier Payable via IAM")
	@DeleteMapping("/operations/payables/{id}")
	public ResponseEntity<?> deletePayable(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.deletePayable(id, auth, org);
	}

	@LogActivity("Get Logistics Dashboard via IAM")
	@GetMapping("/operations/analytics/dashboard")
	public ResponseEntity<?> getLogisticsDashboard(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.getLogisticsDashboard(auth, org);
	}

	// Partnership Invitations (mirrors retailer invitation routes, same core URLs)
	@LogActivity("Create Partnership Invitation for Logistics")
	@PostMapping("/partnership-invitations/create")
	public ResponseEntity<?> createPartnershipInvitation(@RequestBody Map<String, Object> invitationDto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.createPartnershipInvitation(invitationDto, auth, org);
	}

	@LogActivity("Respond to Partnership Invitation for Logistics")
	@PutMapping("/partnership-invitations/{id}/respond")
	public ResponseEntity<?> respondToPartnershipInvitation(@PathVariable Long id,
			@RequestBody Map<String, Object> responseDto, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return logisticsService.respondToPartnershipInvitation(id, responseDto, auth, org);
	}

	@LogActivity("Get Partnership Invitation for Logistics")
	@GetMapping("/partnership-invitations/{id}")
	public ResponseEntity<?> getPartnershipInvitation(@PathVariable Long id,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.getPartnershipInvitation(id, auth, org);
	}

	@LogActivity("Get Sent Partnership Invitations for Logistics")
	@GetMapping("/partnership-invitations/sent")
	public ResponseEntity<?> getSentPartnershipInvitations(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p) {
		return logisticsService.getSentPartnershipInvitations(auth, org, p);
	}

	@LogActivity("Get Received Partnership Invitations for Logistics")
	@GetMapping("/partnership-invitations/received")
	public ResponseEntity<?> getReceivedPartnershipInvitations(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p) {
		return logisticsService.getReceivedPartnershipInvitations(auth, org, p);
	}

	@LogActivity("Get Pending Partnership Invitations for Logistics")
	@GetMapping("/partnership-invitations/pending")
	public ResponseEntity<?> getPendingPartnershipInvitations(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p) {
		return logisticsService.getPendingPartnershipInvitations(auth, org, p);
	}

	@LogActivity("Withdraw Partnership Invitation for Logistics")
	@PutMapping("/partnership-invitations/{id}/withdraw")
	public ResponseEntity<?> withdrawPartnershipInvitation(@PathVariable Long id,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.withdrawPartnershipInvitation(id, auth, org);
	}

	// Logistics shipment list (Core GET /core/shipments/by-logistics-org)
	@LogActivity("Get My Shipments via IAM")
	@GetMapping("/shipments/mine")
	public ResponseEntity<?> getMyShipments(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p) {
		return logisticsService.getMyShipments(auth, org, p);
	}

	@LogActivity("Get All Partnerships for Logistics")
	@GetMapping("/partnerships/all")
	public ResponseEntity<?> getAllPartnerships(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p) {
		return logisticsService.getAllPartnerships(auth, org, p);
	}

	@LogActivity("Update Partnership for Logistics")
	@PutMapping("/partnerships/{id}/update")
	public ResponseEntity<?> updatePartnership(@PathVariable Long id,
			@RequestBody Map<String, Object> partnershipDto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.updatePartnership(id, partnershipDto, auth, org);
	}

	@LogActivity("Update Partnership Status for Logistics")
	@PostMapping("/partnerships/{id}/status")
	public ResponseEntity<?> updatePartnershipStatus(@PathVariable Long id,
			@RequestBody Map<String, Object> statusDto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		String status = statusDto != null ? (String) statusDto.get("status") : null;
		return logisticsService.updatePartnershipStatus(id, status, auth, org);
	}

	// Freight invoice passthroughs (Core P1)
	@LogActivity("Get Freight Invoices by Logistics Org via IAM")
	@GetMapping("/freight-invoices/all")
	public ResponseEntity<?> getFreightInvoicesByLogisticsOrg(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p) {
		return logisticsService.getFreightInvoicesByLogisticsOrg(auth, org, p);
	}

	@LogActivity("Get Freight Invoices by Shipment via IAM")
	@GetMapping("/freight-invoices/shipment/{shipmentId}")
	public ResponseEntity<?> getFreightInvoicesByShipment(@PathVariable Long shipmentId,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return logisticsService.getFreightInvoicesByShipment(shipmentId, auth, org);
	}
}

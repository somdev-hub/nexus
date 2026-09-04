package com.nexus.core.repository;

import java.sql.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.Shipment;
import com.nexus.core.entities.ShipmentStatus;

@Repository
public interface ShipmentRepo extends JpaRepository<Shipment, Long> {

	// Find by retailer organization
	Page<Shipment> findByRetailerOrgAccountId(Long orgId, Pageable pageable);

	// Find by supplier organization
	Page<Shipment> findBySupplierOrgAccountId(Long orgId, Pageable pageable);

	// Find by logistics organization
	Page<Shipment> findByLogisticsOrgAccountId(Long orgId, Pageable pageable);

	// Find by partnership
	Page<Shipment> findByPartnershipPartnershipId(Long partnershipId, Pageable pageable);

	// Find by status
	Page<Shipment> findByRetailerOrgAccountIdAndStatusIn(Long orgId, List<ShipmentStatus> statuses, Pageable pageable);

	// Find by shipment number
	Shipment findByShipmentNumberAndRetailerOrgAccountId(String shipmentNumber, Long orgId);

	// Find by tracking number
	Shipment findByTrackingNumber(String trackingNumber);

	// Find by carrier reference
	Shipment findByCarrierReference(String carrierReference);

	// Find by date range
	@Query("SELECT s FROM Shipment s WHERE s.retailerOrg.accountId = :orgId AND s.pickupDate BETWEEN :startDate AND :endDate")
	Page<Shipment> findByRetailerOrgAndPickupDateBetween(@Param("orgId") Long orgId,
			@Param("startDate") Date startDate, @Param("endDate") Date endDate, Pageable pageable);

	// Find by delivery date range
	@Query("SELECT s FROM Shipment s WHERE s.retailerOrg.accountId = :orgId AND s.deliveryDate BETWEEN :startDate AND :endDate")
	Page<Shipment> findByRetailerOrgAndDeliveryDateBetween(@Param("orgId") Long orgId,
			@Param("startDate") Date startDate, @Param("endDate") Date endDate, Pageable pageable);

	// Find by shipment mode
	Page<Shipment> findByRetailerOrgAccountIdAndShipmentMode(Long orgId, com.nexus.core.entities.ShipmentMode mode,
			Pageable pageable);

	// Find active shipments (not in terminal states)
	@Query("SELECT s FROM Shipment s WHERE s.retailerOrg.accountId = :orgId AND s.status NOT IN (:terminalStatuses)")
	Page<Shipment> findActiveShipmentsByRetailerOrg(@Param("orgId") Long orgId,
			@Param("terminalStatuses") List<ShipmentStatus> terminalStatuses, Pageable pageable);

	// Find shipments requiring attention (exceptions, delays)
	@Query("SELECT s FROM Shipment s WHERE s.retailerOrg.accountId = :orgId AND s.status IN (:attentionStatuses)")
	Page<Shipment> findShipmentsRequiringAttention(@Param("orgId") Long orgId,
			@Param("attentionStatuses") List<ShipmentStatus> attentionStatuses, Pageable pageable);

	// Count by status
	@Query("SELECT COUNT(s) FROM Shipment s WHERE s.retailerOrg.accountId = :orgId AND s.status = :status")
	Long countByRetailerOrgAndStatus(@Param("orgId") Long orgId, @Param("status") ShipmentStatus status);

	// Find by purchase order reference (through stops)
	@Query("SELECT DISTINCT s FROM Shipment s JOIN s.stops st WHERE st.referenceType = 'PURCHASE_ORDER' AND st.referenceId = :poId")
	List<Shipment> findByPurchaseOrderId(@Param("poId") Long poId);
}
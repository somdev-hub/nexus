package com.nexus.core.repository;

import java.sql.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.enums.ShipmentStatus;
public interface ShipmentRepo extends JpaRepository<Shipment, Long> {

	// Find by retailer organization
	Page<Shipment> findByRetailerOrgAccountId(Long orgId, Pageable pageable);

	// Find by supplier organization
	Page<Shipment> findBySupplierOrgAccountId(Long orgId, Pageable pageable);

	// Supplier-owned core shipments (consignment fill / VMI replenishment).
	@Query("""
			SELECT s FROM Shipment s
			WHERE s.supplierOrg.accountId = :orgId
			AND s.referenceType IN :referenceTypes
			""")
	Page<Shipment> findSupplierCoreShipments(@Param("orgId") Long orgId,
			@Param("referenceTypes") List<String> referenceTypes, Pageable pageable);

	// Retailer-visible inbound shipments, optionally filtered by status,
	// reference type, and a free-text search (shipment number, supplier,
	// material, tracking). Excludes DRAFT (not yet handed over to
	// logistics). For PO shipments only partial shipments are shown.
	@Query("""
			SELECT s FROM Shipment s
			LEFT JOIN s.supplierOrg so
			LEFT JOIN s.material m
			WHERE s.retailerOrg.accountId = :orgId
			AND s.status <> 'DRAFT'
			AND (:status IS NULL OR s.status = :status)
			AND (
				:referenceType IS NULL OR :referenceType = ''
				OR COALESCE(s.referenceType, 'PURCHASE_ORDER') = :referenceType
			)
			AND (
				s.referenceType IN ('CONSIGNMENT_FILL', 'VMI_REPLENISHMENT')
				OR ((s.referenceType IS NULL OR s.referenceType = 'PURCHASE_ORDER')
					AND s.isPartialShipment = true)
			)
			AND (
				:search IS NULL OR :search = ''
				OR LOWER(s.shipmentNumber) LIKE CONCAT('%', LOWER(:search), '%')
				OR LOWER(s.carrierName) LIKE CONCAT('%', LOWER(:search), '%')
				OR (so IS NOT NULL AND LOWER(so.name) LIKE CONCAT('%', LOWER(:search), '%'))
				OR (m IS NOT NULL AND LOWER(m.name) LIKE CONCAT('%', LOWER(:search), '%'))
			)
			""")
	Page<Shipment> findRetailerShipments(@Param("orgId") Long orgId,
			@Param("status") ShipmentStatus status,
			@Param("referenceType") String referenceType,
			@Param("search") String search,
			Pageable pageable);

	// Retailer-visible core shipments (consignment fill / VMI replenishment).
	// Exclude DRAFT: only shipments already handed over to logistics are
	// visible to the retailer (they appear in the load board).
	@Query("""
			SELECT s FROM Shipment s
			WHERE s.retailerOrg.accountId = :orgId
			AND s.referenceType IN :referenceTypes
			AND s.status <> 'DRAFT'
			""")
	Page<Shipment> findRetailerCoreShipments(@Param("orgId") Long orgId,
			@Param("referenceTypes") List<String> referenceTypes, Pageable pageable);

	// Retailer-visible PO deliveries (excludes core shipments). Only
	// partial shipments that have been handed over to logistics are shown.
	@Query("""
			SELECT s FROM Shipment s
			WHERE s.retailerOrg.accountId = :orgId
			AND (s.referenceType IS NULL OR s.referenceType = 'PURCHASE_ORDER')
			AND s.isPartialShipment = true
			AND s.status <> 'DRAFT'
			""")
	Page<Shipment> findRetailerPurchaseOrderShipments(@Param("orgId") Long orgId, Pageable pageable);

	// Find shipments by reference type (e.g. CONSIGNMENT_FILL, VMI_REPLENISHMENT).
	@Query("""
			SELECT s FROM Shipment s
			WHERE s.referenceType IN :referenceTypes
			""")
	Page<Shipment> findByReferenceTypeIn(@Param("referenceTypes") List<String> referenceTypes, Pageable pageable);

	// Find by logistics organization
	Page<Shipment> findByLogisticsOrgAccountId(Long orgId, Pageable pageable);

	// Find by partnership
	Page<Shipment> findByPartnershipPartnershipId(Long partnershipId, Pageable pageable);

	// Find by status
	Page<Shipment> findByRetailerOrgAccountIdAndStatusIn(Long orgId, List<ShipmentStatus> statuses, Pageable pageable);

	// Find by shipment number
	Shipment findByShipmentNumberAndRetailerOrgAccountId(String shipmentNumber, Long orgId);


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
	Page<Shipment> findByRetailerOrgAccountIdAndShipmentMode(Long orgId, com.nexus.core.model.enums.ShipmentMode mode,
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

	// Asset operations history (FR-LOG-010 detail view)
	@Query("""
			SELECT s FROM Shipment s
			WHERE s.assignedAssetId = :assetId
			AND s.logisticsOrg.accountId = :orgId
			AND (:status IS NULL OR s.status = :status)
			AND (CAST(:fromDate AS date) IS NULL OR s.deliveryDate IS NULL OR s.deliveryDate >= :fromDate)
			AND (CAST(:toDate AS date) IS NULL OR s.pickupDate IS NULL OR s.pickupDate <= :toDate)
			""")
	Page<Shipment> findByAssetWithFilters(
			@Param("orgId") Long orgId,
			@Param("assetId") Long assetId,
			@Param("status") ShipmentStatus status,
			@Param("fromDate") java.sql.Date fromDate,
			@Param("toDate") java.sql.Date toDate,
			Pageable pageable);

	java.util.List<Shipment> findByAssignedAssetIdAndLogisticsOrgAccountId(Long assetId, Long orgId);

	/** Latest shipments for a driver (any status) — last assignment / last activity. */
	java.util.List<Shipment> findTop10ByAssignedDriverIdAndLogisticsOrgAccountIdOrderByShipmentIdDesc(
			Long driverId, Long orgId);

	/** Delivered-completed trips of a driver — the commission basis. */
	java.util.List<Shipment> findByAssignedDriverIdAndLogisticsOrgAccountIdAndStatusIn(
			Long driverId, Long orgId, java.util.List<ShipmentStatus> statuses);

	Page<Shipment> findByPurchaseOrderPurchaseOrderIdAndIsPartialShipmentTrue(Long purchaseOrderId, Pageable pageable);

	// Legacy consignment-fill / VMI shipments created before supplier/retailer
	// ownership was recorded. Healed on boot by CoreShipmentOwnershipRunner.
	List<Shipment> findBySupplierOrgIsNullAndReferenceTypeIn(List<String> referenceTypes);

	@Query("""
			SELECT s FROM Shipment s
			WHERE s.supplierOrg.accountId = :orgId
			AND (:buyerOrgId IS NULL OR s.retailerOrg.accountId = :buyerOrgId)
			AND (:status IS NULL OR s.status = :status)
			""")
	Page<Shipment> findSupplierVisibleShipments(@Param("orgId") Long orgId,
			@Param("buyerOrgId") Long buyerOrgId,
			@Param("status") ShipmentStatus status, Pageable pageable);

	// Supplier-owned PO deliveries (excludes consignment-fill / VMI core
	// shipments, which surface in their own supplier shipments list).
	@Query("""
			SELECT s FROM Shipment s
			WHERE s.supplierOrg.accountId = :orgId
			AND (:buyerOrgId IS NULL OR s.retailerOrg.accountId = :buyerOrgId)
			AND (:status IS NULL OR s.status = :status)
			AND (s.referenceType IS NULL OR s.referenceType = 'PURCHASE_ORDER')
			""")
	Page<Shipment> findSupplierPurchaseOrderShipments(@Param("orgId") Long orgId,
			@Param("buyerOrgId") Long buyerOrgId,
			@Param("status") ShipmentStatus status, Pageable pageable);

	@Query("SELECT s FROM Shipment s WHERE s.supplierOrg.accountId = :orgId")
	java.util.List<Shipment> findSupplierShipmentsList(@Param("orgId") Long orgId);
}
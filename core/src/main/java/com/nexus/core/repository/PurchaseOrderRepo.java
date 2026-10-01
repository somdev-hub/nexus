package com.nexus.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.nexus.core.model.entities.PurchaseOrder;
import com.nexus.core.model.enums.PurchaseOrderStatus;
public interface PurchaseOrderRepo extends JpaRepository<PurchaseOrder, Long> {

	Page<PurchaseOrder> findByBuyerOrgAccountId(Long orgId, Pageable pageable);

	Optional<PurchaseOrder> findByPurchaseOrderIdAndBuyerOrgAccountId(Long purchaseOrderId, Long orgId);

	Optional<PurchaseOrder> findByPoNumberAndBuyerOrgAccountId(String poNumber, Long orgId);

	List<PurchaseOrder> findByBuyerOrgAccountIdAndStatusIn(Long orgId, List<PurchaseOrderStatus> statuses);

	@Query("SELECT po FROM PurchaseOrder po WHERE po.supplier.supplierId = :supplierId AND po.buyerOrg.accountId = :orgId")
	List<PurchaseOrder> findBySupplierIdAndBuyerOrgId(@Param("supplierId") Long supplierId, @Param("orgId") Long orgId);

	Optional<List<PurchaseOrder>> findBySupplierSupplierIdAndBuyerOrgAccountId(Long supplierId, Long accountId);

	List<PurchaseOrder> findByPartnershipPartnershipIdAndBuyerOrgAccountId(Long partnershipId, Long orgId);

	@Query("SELECT po FROM PurchaseOrder po WHERE po.buyerOrg.accountId = :orgId AND po.parentPoId = :parentPoId ORDER BY po.revisionNumber DESC")
	List<PurchaseOrder> findAmendmentsByParentPoId(@Param("orgId") Long orgId, @Param("parentPoId") Long parentPoId);

	@Query("SELECT po FROM PurchaseOrder po WHERE po.buyerOrg.accountId = :orgId AND po.isBlanketOrder = true")
	List<PurchaseOrder> findBlanketOrdersByOrgId(@Param("orgId") Long orgId);

	@Query("SELECT po FROM PurchaseOrder po WHERE po.buyerOrg.accountId = :orgId AND po.status IN (:statuses)")
	Page<PurchaseOrder> findByOrgIdAndStatusIn(@Param("orgId") Long orgId,
			@Param("statuses") List<PurchaseOrderStatus> statuses, Pageable pageable);

	boolean existsByPoNumberAndBuyerOrgAccountId(String poNumber, Long orgId);

	@Query("SELECT po FROM PurchaseOrder po WHERE po.buyerOrg.accountId = :orgId AND po.isBlanketOrder = true")
	Page<PurchaseOrder> findByBuyerOrgAccountIdAndIsBlanketOrderTrue(@Param("orgId") Long orgId, Pageable pageable);

	@Query("SELECT po FROM PurchaseOrder po WHERE po.buyerOrg.accountId = :orgId AND po.parentPoId = :parentPoId AND po.isBlanketOrder = false ORDER BY po.revisionNumber DESC")
	List<PurchaseOrder> findByParentPoIdAndIsBlanketOrderFalse(@Param("orgId") Long orgId,
			@Param("parentPoId") Long parentPoId);

	// Scoped supplier visibility: supplierOrg == :orgId OR partnership.secondaryOrg == :orgId
	// OR partnership.primaryOrg == :orgId OR (supplierOrg null AND status == SENT_TO_SUPPLIER).
	// Mirrors SupplierOrderServiceImpl.isSupplierOrder including the fallback.
	@Query("""
			SELECT po FROM PurchaseOrder po
			LEFT JOIN po.supplierOrg so LEFT JOIN po.partnership p
			LEFT JOIN p.secondaryOrg sec LEFT JOIN p.primaryOrg pri
			WHERE po.purchaseOrderId = :id
			AND (so.accountId = :orgId
				OR sec.accountId = :orgId
				OR pri.accountId = :orgId
				OR (po.supplierOrg IS NULL AND po.status = com.nexus.core.model.enums.PurchaseOrderStatus.SENT_TO_SUPPLIER))
			""")
	Optional<PurchaseOrder> findSupplierVisibleOrderById(@Param("id") Long id, @Param("orgId") Long orgId);

	@Query("""
			SELECT po FROM PurchaseOrder po
			LEFT JOIN po.supplierOrg so LEFT JOIN po.partnership p
			LEFT JOIN p.secondaryOrg sec LEFT JOIN p.primaryOrg pri
			LEFT JOIN po.buyerOrg b
			WHERE (so.accountId = :orgId
				OR sec.accountId = :orgId
				OR pri.accountId = :orgId
				OR (po.supplierOrg IS NULL AND po.status = com.nexus.core.model.enums.PurchaseOrderStatus.SENT_TO_SUPPLIER))
			AND (:status IS NULL OR po.status = :status)
			AND (:poNumber IS NULL OR LOWER(po.poNumber) LIKE LOWER(CONCAT('%', :poNumber, '%')))
			AND (:buyerOrgId IS NULL OR b.accountId = :buyerOrgId)
			""")
	Page<PurchaseOrder> findSupplierVisibleOrders(@Param("orgId") Long orgId,
			@Param("status") PurchaseOrderStatus status,
			@Param("poNumber") String poNumber,
			@Param("buyerOrgId") Long buyerOrgId, Pageable pageable);

	@Query("""
			SELECT po FROM PurchaseOrder po
			LEFT JOIN po.supplierOrg so LEFT JOIN po.partnership p
			LEFT JOIN p.secondaryOrg sec LEFT JOIN p.primaryOrg pri
			WHERE (so.accountId = :orgId
				OR sec.accountId = :orgId
				OR pri.accountId = :orgId
				OR (po.supplierOrg IS NULL AND po.status = com.nexus.core.model.enums.PurchaseOrderStatus.SENT_TO_SUPPLIER))
			""")
	List<PurchaseOrder> findSupplierVisibleOrdersList(@Param("orgId") Long orgId);
}
package com.nexus.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.PurchaseOrder;
import com.nexus.core.entities.PurchaseOrderStatus;

@Repository
public interface PurchaseOrderRepo extends JpaRepository<PurchaseOrder, Long> {

	Page<PurchaseOrder> findByBuyerOrgId(Long orgId, Pageable pageable);

	Optional<PurchaseOrder> findByPurchaseOrderIdAndBuyerOrgId(Long purchaseOrderId, Long orgId);

	Optional<PurchaseOrder> findByPoNumberAndBuyerOrgId(String poNumber, Long orgId);

	List<PurchaseOrder> findByBuyerOrgIdAndStatusIn(Long orgId, List<PurchaseOrderStatus> statuses);

	List<PurchaseOrder> findBySupplierIdAndBuyerOrgId(Long supplierId, Long orgId);

	List<PurchaseOrder> findByPartnershipIdAndBuyerOrgId(Long partnershipId, Long orgId);

	@Query("SELECT po FROM PurchaseOrder po WHERE po.buyerOrg.accountId = :orgId AND po.parentPoId = :parentPoId ORDER BY po.revisionNumber DESC")
	List<PurchaseOrder> findAmendmentsByParentPoId(@Param("orgId") Long orgId, @Param("parentPoId") Long parentPoId);

	@Query("SELECT po FROM PurchaseOrder po WHERE po.buyerOrg.accountId = :orgId AND po.isBlanketOrder = true")
	List<PurchaseOrder> findBlanketOrdersByOrgId(@Param("orgId") Long orgId);

	@Query("SELECT po FROM PurchaseOrder po WHERE po.buyerOrg.accountId = :orgId AND po.status IN (:statuses)")
	Page<PurchaseOrder> findByOrgIdAndStatusIn(@Param("orgId") Long orgId,
			@Param("statuses") List<PurchaseOrderStatus> statuses, Pageable pageable);

	boolean existsByPoNumberAndBuyerOrgId(String poNumber, Long orgId);
}
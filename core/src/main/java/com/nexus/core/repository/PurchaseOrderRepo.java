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
}
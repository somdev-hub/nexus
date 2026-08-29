package com.nexus.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.GoodsReceipt;
import com.nexus.core.entities.GoodsReceiptStatus;

@Repository
public interface GoodsReceiptRepo extends JpaRepository<GoodsReceipt, Long> {

	Page<GoodsReceipt> findByPurchaseOrderPurchaseOrderId(Long purchaseOrderId, Pageable pageable);

	Optional<List<GoodsReceipt>> findByPurchaseOrderPurchaseOrderId(Long purchaseOrderId);

	Page<GoodsReceipt> findBySupplierSupplierId(Long supplierId, Pageable pageable);

	Page<GoodsReceipt> findByStatus(GoodsReceiptStatus status, Pageable pageable);

	@Query("SELECT gr FROM GoodsReceipt gr WHERE gr.purchaseOrder.purchaseOrderId = :poId AND gr.status = :status")
	Page<GoodsReceipt> findByPurchaseOrderAndStatus(@Param("poId") Long poId,
			@Param("status") GoodsReceiptStatus status, Pageable pageable);

	@Query("SELECT gr FROM GoodsReceipt gr WHERE gr.purchaseOrder.buyerOrg.accountId = :orgId")
	Page<GoodsReceipt> findByPurchaseOrderBuyerOrgId(@Param("orgId") Long orgId, Pageable pageable);

	@Query("SELECT gr FROM GoodsReceipt gr WHERE gr.goodsReceiptId = :id AND gr.purchaseOrder.buyerOrg.accountId = :orgId")
	GoodsReceipt findByGoodsReceiptIdAndPurchaseOrderBuyerOrgId(@Param("id") Long id, @Param("orgId") Long orgId);

	boolean existsByGrNumber(String grNumber);
}
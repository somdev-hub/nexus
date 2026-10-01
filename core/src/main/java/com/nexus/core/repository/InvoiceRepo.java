package com.nexus.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.nexus.core.model.entities.Invoice;
import com.nexus.core.model.enums.InvoiceStatus;
public interface InvoiceRepo extends JpaRepository<Invoice, Long> {

	Page<Invoice> findByPurchaseOrderPurchaseOrderId(Long purchaseOrderId, Pageable pageable);

	Optional<List<Invoice>> findByPurchaseOrderPurchaseOrderId(Long purchaseOrderId);

	Page<Invoice> findBySupplierSupplierId(Long supplierId, Pageable pageable);

	Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

	@Query("SELECT i FROM Invoice i WHERE i.purchaseOrder.purchaseOrderId = :poId AND i.status = :status")
	Page<Invoice> findByPurchaseOrderAndStatus(@Param("poId") Long poId, @Param("status") InvoiceStatus status,
			Pageable pageable);

	@Query("SELECT i FROM Invoice i WHERE i.purchaseOrder.buyerOrg.accountId = :orgId")
	Page<Invoice> findByPurchaseOrderBuyerOrgId(@Param("orgId") Long orgId, Pageable pageable);

	@Query("SELECT i FROM Invoice i WHERE i.invoiceId = :id AND i.purchaseOrder.buyerOrg.accountId = :orgId")
	Invoice findByInvoiceIdAndPurchaseOrderBuyerOrgId(@Param("id") Long id, @Param("orgId") Long orgId);

	boolean existsByInvoiceNumber(String invoiceNumber);

	// Invoices visible to a supplier org via the linked purchase order's supplier scope
	// (same visibility predicate as PurchaseOrderRepo.findSupplierVisibleOrders).
	@Query("""
			SELECT i FROM Invoice i JOIN i.purchaseOrder po
			LEFT JOIN po.supplierOrg so LEFT JOIN po.partnership p
			LEFT JOIN p.secondaryOrg sec LEFT JOIN p.primaryOrg pri
			LEFT JOIN po.buyerOrg b
			WHERE (so.accountId = :orgId
				OR sec.accountId = :orgId
				OR pri.accountId = :orgId
				OR (po.supplierOrg IS NULL AND po.status = com.nexus.core.model.enums.PurchaseOrderStatus.SENT_TO_SUPPLIER))
			AND (:buyerOrgId IS NULL OR b.accountId = :buyerOrgId)
			AND (:status IS NULL OR i.status = :status)
			""")
	Page<Invoice> findSupplierVisibleInvoices(@Param("orgId") Long orgId,
			@Param("buyerOrgId") Long buyerOrgId,
			@Param("status") InvoiceStatus status, Pageable pageable);
}
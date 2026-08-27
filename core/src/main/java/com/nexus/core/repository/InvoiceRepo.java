package com.nexus.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.Invoice;
import com.nexus.core.entities.InvoiceStatus;

@Repository
public interface InvoiceRepo extends JpaRepository<Invoice, Long> {

	Page<Invoice> findByPurchaseOrderPurchaseOrderId(Long purchaseOrderId, Pageable pageable);

	Page<Invoice> findBySupplierSupplierId(Long supplierId, Pageable pageable);

	Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

	@Query("SELECT i FROM Invoice i WHERE i.purchaseOrder.purchaseOrderId = :poId AND i.status = :status")
	Page<Invoice> findByPurchaseOrderAndStatus(@Param("poId") Long poId, @Param("status") InvoiceStatus status,
			Pageable pageable);

	boolean existsByInvoiceNumber(String invoiceNumber);
}
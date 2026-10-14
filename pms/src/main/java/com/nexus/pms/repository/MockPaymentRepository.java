package com.nexus.pms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.pms.model.entities.MockPayment;
import com.nexus.pms.model.enums.MockPaymentStatus;

@Repository
public interface MockPaymentRepository extends JpaRepository<MockPayment, Long> {

    Optional<MockPayment> findByIdempotencyKey(String idempotencyKey);

    Optional<MockPayment> findByTransactionReference(String transactionReference);

    List<MockPayment> findByInvoiceIdOrderByPaymentIdDesc(Long invoiceId);

    Page<MockPayment> findByOrgId(String orgId, Pageable pageable);

    Page<MockPayment> findByOrgIdAndStatus(String orgId, MockPaymentStatus status, Pageable pageable);

    Page<MockPayment> findByStatus(MockPaymentStatus status, Pageable pageable);
}

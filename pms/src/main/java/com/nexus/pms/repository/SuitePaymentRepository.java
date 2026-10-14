package com.nexus.pms.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.pms.model.entities.SuitePayment;
import com.nexus.pms.model.enums.SuitePaymentStatus;

@Repository
public interface SuitePaymentRepository extends JpaRepository<SuitePayment, Long> {

    Optional<SuitePayment> findByIdempotencyKey(String idempotencyKey);

    Optional<SuitePayment> findByIntentRef(String intentRef);

    Page<SuitePayment> findByOrgId(String orgId, Pageable pageable);

    Page<SuitePayment> findByOrgIdAndStatus(String orgId, SuitePaymentStatus status, Pageable pageable);
}

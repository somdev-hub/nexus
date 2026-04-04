package com.nexus.pms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.pms.model.entities.PaymentMethodEntity;

/**
 * Repository for PaymentMethodEntity.
 * Provides database operations for payment method records.
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethodEntity, Long> {

    /**
     * Find a payment method by its ID.
     * Inherited from JpaRepository.
     */
}

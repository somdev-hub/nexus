package com.nexus.pms.repository;

import com.nexus.pms.model.entities.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for IdempotencyRecord.
 * Manages idempotency records to prevent duplicate payment processing.
 */
@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    /**
     * Find an idempotency record by idempotency key.
     *
     * @param idempotencyKey The idempotency key
     * @return IdempotencyRecord if found, empty otherwise
     */
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    /**
     * Check if an idempotency key exists.
     *
     * @param idempotencyKey The idempotency key
     * @return true if exists, false otherwise
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
}

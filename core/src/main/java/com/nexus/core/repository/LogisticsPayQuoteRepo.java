package com.nexus.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.model.entities.LogisticsPayQuote;

@Repository
public interface LogisticsPayQuoteRepo extends JpaRepository<LogisticsPayQuote, Long> {

	Optional<LogisticsPayQuote> findByQuoteRef(String quoteRef);

	Optional<LogisticsPayQuote> findByIdempotencyKey(String idempotencyKey);
}

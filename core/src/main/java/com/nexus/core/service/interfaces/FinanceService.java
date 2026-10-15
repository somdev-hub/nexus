package com.nexus.core.service.interfaces;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface FinanceService {

	ResponseEntity<?> retailerSummary(String period);

	ResponseEntity<?> retailerTransactions(String kind, String status, String search,
			java.sql.Date from, java.sql.Date to, Pageable pageable);

	ResponseEntity<?> supplierSummary(String period);

	ResponseEntity<?> supplierTransactions(String kind, String status, String search,
			java.sql.Date from, java.sql.Date to, Pageable pageable);

	ResponseEntity<?> logisticsSummary(String period);

	ResponseEntity<?> logisticsTransactions(String kind, String status, String search,
			java.sql.Date from, java.sql.Date to, Pageable pageable);
}

package com.nexus.core.service.interfaces;

import org.springframework.http.ResponseEntity;

public interface AtpService {

    ResponseEntity<?> getAvailableToPromise(Long catalogId, Double requestedQuantity);

    ResponseEntity<?> getAtpForProductLine(String productLine, java.sql.Date date);
}

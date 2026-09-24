package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.CollaborativeForecastDto;

import java.sql.Date;

public interface CollaborativeForecastService {

    ResponseEntity<?> createForecast(CollaborativeForecastDto dto);

    ResponseEntity<?> getForecast(Long id);

    ResponseEntity<?> getAllForecasts(Long retailerOrgId, Long catalogId, String status, Date periodStart, Date periodEnd, Pageable pageable);

    ResponseEntity<?> updateForecast(Long id, CollaborativeForecastDto dto);

    ResponseEntity<?> transitionStatus(Long id, String newStatus);

    ResponseEntity<?> deleteForecast(Long id);
}

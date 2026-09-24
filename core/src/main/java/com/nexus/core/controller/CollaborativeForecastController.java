package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.CollaborativeForecastDto;
import com.nexus.core.service.CollaborativeForecastService;

import lombok.RequiredArgsConstructor;

import java.sql.Date;

@RestController
@RequestMapping("/core/supplier/forecasts")
@RequiredArgsConstructor
public class CollaborativeForecastController {

    private final CollaborativeForecastService forecastService;

    @PostMapping("/create")
    @LogActivity("Create Collaborative Forecast")
    public ResponseEntity<?> createForecast(@Valid @RequestBody CollaborativeForecastDto dto) {
        return forecastService.createForecast(dto);
    }

    @GetMapping("/{id}")
    @LogActivity("Get Collaborative Forecast")
    public ResponseEntity<?> getForecast(@PathVariable Long id) {
        return forecastService.getForecast(id);
    }

    @GetMapping("/all")
    @LogActivity("Get All Collaborative Forecasts")
    public ResponseEntity<?> getAllForecasts(
            @RequestParam(required = false) Long retailerOrgId,
            @RequestParam(required = false) Long catalogId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Date periodStart,
            @RequestParam(required = false) Date periodEnd,
            @PageableDefault(size = 20) Pageable pageable) {
        return forecastService.getAllForecasts(retailerOrgId, catalogId, status, periodStart, periodEnd, pageable);
    }

    @PutMapping("/{id}/update")
    @LogActivity("Update Collaborative Forecast")
    public ResponseEntity<?> updateForecast(@PathVariable Long id, @RequestBody CollaborativeForecastDto dto) {
        return forecastService.updateForecast(id, dto);
    }

    @PutMapping("/{id}/status")
    @LogActivity("Transition Forecast Status")
    public ResponseEntity<?> transitionStatus(@PathVariable Long id, @RequestParam String newStatus) {
        return forecastService.transitionStatus(id, newStatus);
    }

    @DeleteMapping("/{id}")
    @LogActivity("Delete Collaborative Forecast")
    public ResponseEntity<?> deleteForecast(@PathVariable Long id) {
        return forecastService.deleteForecast(id);
    }
}

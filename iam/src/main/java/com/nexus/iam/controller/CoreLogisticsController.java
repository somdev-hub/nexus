package com.nexus.iam.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core Logistics Controller
 * <p>
 * Handles logistics-specific Core module APIs through IAM gateway.
 * Frontend calls these endpoints for logistics operations.
 */
@Slf4j
@RestController
@RequestMapping("/iam/core/logistics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CoreLogisticsController {

}
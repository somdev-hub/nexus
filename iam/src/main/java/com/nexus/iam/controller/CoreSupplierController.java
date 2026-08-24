package com.nexus.iam.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core Supplier Controller
 * <p>
 * Handles supplier-specific Core module APIs through IAM gateway.
 * Frontend calls these endpoints for supplier operations.
 */
@Slf4j
@RestController
@RequestMapping("/iam/core/supplier")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CoreSupplierController {

}
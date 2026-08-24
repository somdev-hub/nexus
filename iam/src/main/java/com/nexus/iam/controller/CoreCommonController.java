package com.nexus.iam.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core Common Controller
 * <p>
 * Handles common Core module APIs through IAM gateway that are not specific
 * to retailer, supplier, or logistics.
 */
@Slf4j
@RestController
@RequestMapping("/iam/core/common")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CoreCommonController {

}
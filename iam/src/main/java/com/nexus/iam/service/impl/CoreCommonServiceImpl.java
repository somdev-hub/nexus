package com.nexus.iam.service.impl;

import org.springframework.stereotype.Service;

import com.nexus.iam.service.CoreCommonService;

import lombok.RequiredArgsConstructor;

/**
 * Core Common Service Implementation
 * <p>
 * Handles common Core module operations through IAM gateway that are not
 * specific to retailer, supplier, or logistics.
 * All HTTP calls to Core module are handled here using RestService.
 */
@Service
@RequiredArgsConstructor
public class CoreCommonServiceImpl implements CoreCommonService {

}
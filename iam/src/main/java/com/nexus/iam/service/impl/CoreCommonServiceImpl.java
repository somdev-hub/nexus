package com.nexus.iam.service.impl;

import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.iam.service.CoreCommonService;
import com.nexus.iam.utils.CommonUtils;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;

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
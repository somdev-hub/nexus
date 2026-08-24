package com.nexus.core.service;

import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.OrderDto;

public interface OrderService {
	public ResponseEntity<?> addOrder(OrderDto orderDto);

	public ResponseEntity<?> getOrderByIdAndOrg(Long id, Long orgId);

	public ResponseEntity<?> getAllOrdersByOrgId(Long orgId);
}
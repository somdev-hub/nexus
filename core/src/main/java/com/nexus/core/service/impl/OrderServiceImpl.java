package com.nexus.core.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.core.entities.Material;
import com.nexus.core.entities.Order;
import com.nexus.core.entities.Product;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.OrderDto;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.OrderRepo;
import com.nexus.core.repository.ProductRepo;
import com.nexus.core.service.OrderService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private final OrderRepo orderRepo;
	private final ModelMapper modelMapper;
	private final MaterialRepo materialRepo;
	private final ProductRepo productRepo;

	@Override
	public ResponseEntity<?> addOrder(OrderDto orderDto) {
		Order order = modelMapper.map(orderDto, Order.class);

		// Set material if provided
		if (orderDto.getMaterialId() != null) {
			Material material = materialRepo.findByMaterialIdAndOrg(orderDto.getMaterialId(), orderDto.getBuyerOrgId())
					.orElseThrow(
							() -> new ResourceNotFoundException("Material", "materialId", orderDto.getMaterialId()));
			order.setMaterial(material);
		}

		// Set product if provided
		if (orderDto.getProductId() != null) {
			Product product = productRepo.findByProductIdAndOrg(orderDto.getProductId(), orderDto.getBuyerOrgId())
					.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", orderDto.getProductId()));
			order.setProduct(product);
		}

		Order savedOrder = orderRepo.save(order);
		return new ResponseEntity<>(modelMapper.map(savedOrder, OrderDto.class), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getOrderByIdAndOrg(Long id, Long orgId) {
		Order order = orderRepo.findByOrderIdAndBuyerOrgId(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", id));
		return new ResponseEntity<>(modelMapper.map(order, OrderDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllOrdersByOrgId(Long orgId, Pageable pageable) {
		Page<Order> orders = orderRepo.findByBuyerOrgId(orgId, pageable);
		Page<OrderDto> orderDtos = orders.map(o -> modelMapper.map(o, OrderDto.class));
		return new ResponseEntity<>(orderDtos, HttpStatus.OK);
	}

}
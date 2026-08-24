package com.nexus.core.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.nexus.core.entities.Material;
import com.nexus.core.entities.Order;
import com.nexus.core.entities.Product;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.ErrorResponse;
import com.nexus.core.payload.OrderDto;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.OrderRepo;
import com.nexus.core.repository.ProductRepo;
import com.nexus.core.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private OrderRepo orderRepo;

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private MaterialRepo materialRepo;

	@Autowired
	private ProductRepo productRepo;

	@Override
	public ResponseEntity<?> addOrder(OrderDto orderDto) {
		if (ObjectUtils.isEmpty(orderDto) || ObjectUtils.isEmpty(orderDto.getBuyerOrgId())) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse("Empty Details sent", HttpStatus.BAD_REQUEST.value(),
							Timestamp.valueOf(LocalDateTime.now()), "Necessary details are not sent!"),
					HttpStatus.BAD_REQUEST);

		}
		try {

			Order order = modelMapper.map(orderDto, Order.class);

			// Set material if provided
			if (orderDto.getMaterialId() != null) {
				Material material = materialRepo.findById(orderDto.getMaterialId()).orElse(null);
				order.setMaterial(material);
			}

			// Set product if provided
			if (orderDto.getProductId() != null) {
				Product product = productRepo.findById(orderDto.getProductId()).orElse(null);
				order.setProduct(product);
			}

			Order savedOrder = orderRepo.save(order);
			return new ResponseEntity<>(modelMapper.map(savedOrder, OrderDto.class), HttpStatus.CREATED);

		} catch (Exception e) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse("Failed to add order", HttpStatus.INTERNAL_SERVER_ERROR.value(),
							Timestamp.valueOf(LocalDateTime.now()), e.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@Override
	public ResponseEntity<?> getOrderByIdAndOrg(Long id, Long orgId) {
		if (ObjectUtils.isEmpty(id) || ObjectUtils.isEmpty(orgId)) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse(
							"Order ID and Organization ID cannot be null or empty",
							HttpStatus.BAD_REQUEST.value(),
							Timestamp.valueOf(LocalDateTime.now()),
							"Invalid Order ID or Organization ID"),
					HttpStatus.BAD_REQUEST);
		}
		try {
			Order order = orderRepo.findByIdAndBuyerOrg(id, orgId).orElse(null);
			if (ObjectUtils.isEmpty(order)) {
				return new ResponseEntity<ErrorResponse>(
						new ErrorResponse(
								"Order not found in organization",
								HttpStatus.NOT_FOUND.value(),
								Timestamp.valueOf(LocalDateTime.now()),
								"No order found with the given ID in the organization"),
						HttpStatus.NOT_FOUND);
			}
			return new ResponseEntity<>(modelMapper.map(order, OrderDto.class), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse(
							"Failed to retrieve order",
							HttpStatus.INTERNAL_SERVER_ERROR.value(),
							Timestamp.valueOf(LocalDateTime.now()),
							e.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<?> getAllOrdersByOrgId(Long orgId) {
		if (ObjectUtils.isEmpty(orgId)) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse(
							"Organization ID cannot be null or empty",
							HttpStatus.BAD_REQUEST.value(),
							Timestamp.valueOf(LocalDateTime.now()),
							"Invalid Organization ID"),
					HttpStatus.BAD_REQUEST);

		}
		try {
			List<Order> orders = orderRepo.findByBuyerOrg(orgId).orElseThrow(() -> {
				throw new ResourceNotFoundException("Orders", "orgId", orgId);
			});
			List<OrderDto> orderDtos = new java.util.ArrayList<>();
			for (Order order : orders) {
				orderDtos.add(modelMapper.map(order, OrderDto.class));
			}
			return new ResponseEntity<>(orderDtos, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse(
							"Failed to retrieve orders",
							HttpStatus.INTERNAL_SERVER_ERROR.value(),
							Timestamp.valueOf(LocalDateTime.now()),
							e.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
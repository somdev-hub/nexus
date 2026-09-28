package com.nexus.core.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nexus.core.model.entities.Order;
public interface OrderRepo extends JpaRepository<Order, Long> {

	Page<Order> findByBuyerOrgId(Long orgId, Pageable pageable);

	Optional<Order> findByOrderIdAndBuyerOrgId(Long orderId, Long orgId);

}
package com.nexus.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.Order;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {

	Optional<List<Order>> findByBuyerOrg(Long orgId);

	Optional<Order> findByIdAndBuyerOrg(Long id, Long orgId);

}
package com.nexus.core.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.Product;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {

	Optional<Product> findByProductId(Long productId);

	Page<Product> findByOrg(Long orgId, Pageable pageable);

	Optional<Product> findByProductIdAndOrg(Long productId, Long orgId);

}

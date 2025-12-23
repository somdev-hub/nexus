package com.nexus.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.Product;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {

    Optional<Product> findById(Long id);

    Optional<List<Product>> findByOrg(Long orgId);

}

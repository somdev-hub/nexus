package com.nexus.nexusbuddy.repository;

import com.nexus.nexusbuddy.model.entities.ClientConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientConfigRepository extends JpaRepository<ClientConfig, Long> {
    
    Optional<ClientConfig> findByClientName(String clientName);
    
    Page<ClientConfig> findByIsActiveTrue(Pageable pageable);
    
    boolean existsByClientName(String clientName);
}
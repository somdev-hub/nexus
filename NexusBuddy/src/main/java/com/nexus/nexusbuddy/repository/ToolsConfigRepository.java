package com.nexus.nexusbuddy.repository;

import com.nexus.nexusbuddy.model.entities.ToolsConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ToolsConfigRepository extends JpaRepository<ToolsConfig, Long> {
    
    Optional<ToolsConfig> findByToolName(String toolName);
    
    Page<ToolsConfig> findByIsActiveTrue(Pageable pageable);
    
    Page<ToolsConfig> findByClientConfigClientConfigId(Long clientConfigId, Pageable pageable);

    List<ToolsConfig> findByIsActiveTrueAndClientConfigClientConfigIdIn(Collection<Long> clientConfigIds);
    
    boolean existsByToolName(String toolName);
}
package com.nexus.nexusbuddy.repository;

import com.nexus.nexusbuddy.model.entities.ToolsConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolsConfigRepository extends JpaRepository<ToolsConfig, Long> {
    
    Optional<ToolsConfig> findByToolName(String toolName);
    
    List<ToolsConfig> findByIsActiveTrue();
    
    List<ToolsConfig> findByClientConfigClientConfigId(Long clientConfigId);
    
    boolean existsByToolName(String toolName);
}
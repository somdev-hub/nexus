package com.nexus.nexusbuddy.repository;

import com.nexus.nexusbuddy.model.entities.ToolsParamConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolsParamConfigRepository extends JpaRepository<ToolsParamConfig, Long> {
    
    Optional<ToolsParamConfig> findByParamName(String paramName);
    
    List<ToolsParamConfig> findByIsActiveTrue();
    
    List<ToolsParamConfig> findByToolsConfigToolsConfigId(Long toolsConfigId);
    
    boolean existsByParamNameAndToolsConfigToolsConfigId(String paramName, Long toolsConfigId);
}
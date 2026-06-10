package com.nexus.cms.repository;

import com.nexus.cms.model.entities.EventTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventTemplateRepo extends JpaRepository<EventTemplate, Long> {
    List<EventTemplate> findByOrgIdAndIsActiveTrue(Long orgId);

    Optional<EventTemplate> findByTemplateNameAndOrgIdAndIsActiveTrue(String templateName, Long orgId);
}

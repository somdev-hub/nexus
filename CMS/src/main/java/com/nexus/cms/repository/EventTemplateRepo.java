package com.nexus.cms.repository;

import com.nexus.cms.model.entities.EventTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventTemplateRepo extends JpaRepository<EventTemplate, Long> {
}

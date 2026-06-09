package com.nexus.cms.repository;

import com.nexus.cms.model.entities.TemplateParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateParamRepo extends JpaRepository<TemplateParam, Long> {
}

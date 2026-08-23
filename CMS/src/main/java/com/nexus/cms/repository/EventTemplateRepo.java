package com.nexus.cms.repository;

import com.nexus.cms.model.entities.EventTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventTemplateRepo extends JpaRepository<EventTemplate, Long> {
    List<EventTemplate> findByOrgIdAndIsActiveTrue(Long orgId);

    // use join fetch
    @Query(
            """
                                    SELECT e FROM EventTemplate e
                                    LEFT JOIN FETCH e.templateParams
                                    WHERE e.templateName = :templateName AND e.orgId = :orgId AND e.isActive = true
                    """
    )
    Optional<EventTemplate> findByTemplateNameAndOrgIdAndIsActiveTrue(String templateName, Long orgId);

    boolean existsByTemplateNameAndOrgIdAndIsActiveTrue(String templateName, Long orgId);

    List<EventTemplate> findByOrgId(Long orgId);
}

package com.nexus.core.repository;

import com.nexus.core.entities.ConsolidationGroup;
import com.nexus.core.entities.ConsolidationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsolidationGroupRepo extends JpaRepository<ConsolidationGroup, Long> {

    @Query("SELECT g FROM ConsolidationGroup g WHERE g.groupId = :id AND g.logisticsOrg.accountId = :orgId")
    Optional<ConsolidationGroup> findByIdAndOrg(@Param("id") Long id, @Param("orgId") Long orgId);

    @Query("""
            SELECT g FROM ConsolidationGroup g
            WHERE g.logisticsOrg.accountId = :orgId
            AND (:status IS NULL OR g.status = :status)
            AND (:search IS NULL OR LOWER(g.groupNumber) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<ConsolidationGroup> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("status") ConsolidationStatus status,
            @Param("search") String search,
            Pageable pageable);
}

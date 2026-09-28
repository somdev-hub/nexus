package com.nexus.core.repository;

import com.nexus.core.model.enums.FleetAssetType;
import com.nexus.core.model.entities.FreightRate;
import com.nexus.core.model.enums.RateType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface FreightRateRepo extends JpaRepository<FreightRate, Long> {

    @Query("SELECT r FROM FreightRate r WHERE r.rateId = :id AND r.logisticsOrg.accountId = :orgId")
    Optional<FreightRate> findByIdAndOrg(@Param("id") Long id, @Param("orgId") Long orgId);

    Optional<FreightRate> findByRateCodeAndLogisticsOrgAccountId(String rateCode, Long orgId);

    @Query("""
            SELECT r FROM FreightRate r
            WHERE r.logisticsOrg.accountId = :orgId
            AND (:rateType IS NULL OR r.rateType = :rateType)
            AND (:equipmentType IS NULL OR r.equipmentType = :equipmentType)
            AND (:search IS NULL OR LOWER(r.rateCode) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(r.originLane) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(r.destinationLane) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<FreightRate> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("rateType") RateType rateType,
            @Param("equipmentType") FleetAssetType equipmentType,
            @Param("search") String search,
            Pageable pageable);
}

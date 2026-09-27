package com.nexus.core.repository;

import com.nexus.core.entities.Driver;
import com.nexus.core.entities.DriverStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverRepo extends JpaRepository<Driver, Long> {

    Optional<Driver> findByDriverIdAndLogisticsOrgAccountId(Long id, Long orgId);

    Optional<Driver> findByDriverCodeAndLogisticsOrgAccountId(String driverCode, Long orgId);

    @Query("""
            SELECT d FROM Driver d
            WHERE d.logisticsOrg.accountId = :orgId
            AND (:status IS NULL OR d.status = :status)
            AND (:search IS NULL OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(d.driverCode) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Driver> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("status") DriverStatus status,
            @Param("search") String search,
            Pageable pageable);
}

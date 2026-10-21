package com.nexus.core.repository;

import com.nexus.core.model.entities.DriverCommission;
import com.nexus.core.model.enums.DriverCommissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DriverCommissionRepo extends JpaRepository<DriverCommission, Long> {

    Optional<DriverCommission> findByDriverIdAndShipmentId(Long driverId, Long shipmentId);

    Optional<DriverCommission> findByCommissionIdAndDriverId(Long commissionId, Long driverId);

    Page<DriverCommission> findByDriverIdAndLogisticsOrgId(Long driverId, Long orgId, Pageable pageable);

    Page<DriverCommission> findByDriverIdAndLogisticsOrgIdAndStatus(
            Long driverId, Long orgId, DriverCommissionStatus status, Pageable pageable);

    List<DriverCommission> findByStatusAndDriverIdIn(
            DriverCommissionStatus status, Collection<Long> driverIds);

    List<DriverCommission> findByStatusAndDriverIdInAndLogisticsOrgId(
            DriverCommissionStatus status, Collection<Long> driverIds, Long orgId);

    /** All non-paid payout obligations of a logistics org, regardless of driver. */
    List<DriverCommission> findByLogisticsOrgIdAndStatus(Long orgId, DriverCommissionStatus status);
}

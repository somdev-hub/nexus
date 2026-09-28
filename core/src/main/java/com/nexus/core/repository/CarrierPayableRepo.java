package com.nexus.core.repository;

import com.nexus.core.model.entities.CarrierPayable;
import com.nexus.core.model.enums.PayableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface CarrierPayableRepo extends JpaRepository<CarrierPayable, Long> {

    @Query("SELECT p FROM CarrierPayable p WHERE p.payableId = :id AND p.logisticsOrg.accountId = :orgId")
    Optional<CarrierPayable> findByIdAndOrg(@Param("id") Long id, @Param("orgId") Long orgId);

    @Query("""
            SELECT p FROM CarrierPayable p
            WHERE p.logisticsOrg.accountId = :orgId
            AND (:shipmentId IS NULL OR p.shipment.shipmentId = :shipmentId)
            AND (:status IS NULL OR p.status = :status)
            AND (:search IS NULL OR LOWER(p.payableNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(p.carrierName) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<CarrierPayable> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("shipmentId") Long shipmentId,
            @Param("status") PayableStatus status,
            @Param("search") String search,
            Pageable pageable);
}

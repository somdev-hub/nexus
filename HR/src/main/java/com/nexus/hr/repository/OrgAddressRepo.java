package com.nexus.hr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.hr.model.entities.OrgAddress;

@Repository
public interface OrgAddressRepo extends JpaRepository<OrgAddress, Long> {

    List<OrgAddress> findByOrgId(Long orgId);

    List<OrgAddress> findByOrgIdAndIsActive(Long orgId, Boolean isActive);

    Optional<OrgAddress> findByOrgAddressIdAndOrgId(Long orgAddressId, Long orgId);

    List<OrgAddress> findByOrgIdAndIsDefaultShippingTrue(Long orgId);

    List<OrgAddress> findByOrgIdAndIsDefaultBillingTrue(Long orgId);

}

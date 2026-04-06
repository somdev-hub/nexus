package com.nexus.hr.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.hr.model.entities.OrgAccountInfo;

@Repository
public interface OrgAccountInfoRepo extends JpaRepository<OrgAccountInfo, Long> {

    Optional<OrgAccountInfo> findByOrgId(Long orgId);

}

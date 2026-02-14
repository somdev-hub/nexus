package com.nexus.hr.repository;

import com.nexus.hr.model.entities.HrEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HrEntityRepo extends JpaRepository<HrEntity, Long> {

    @Query(value = "SELECT COUNT(*) FROM t_hr_entity WHERE org = :orgId AND on_notice_period=true", nativeQuery = true)
    Integer getAllWhoAreOnNoticePeriod(Long orgId);

    Optional<HrEntity> findByEmployeeId(Long id);
}

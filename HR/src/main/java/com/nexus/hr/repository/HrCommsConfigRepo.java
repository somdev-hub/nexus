package com.nexus.hr.repository;

import com.nexus.hr.model.entities.HrCommsConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HrCommsConfigRepo extends JpaRepository<HrCommsConfig, Long> {

    Optional<HrCommsConfig> findByCommsTriggerPoint(String triggerPoint);
}

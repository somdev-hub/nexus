package com.nexus.hr.repository;

import com.nexus.hr.model.entities.WOWOConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WOWOConfigRepo extends JpaRepository<WOWOConfig, Long> {

    Optional<WOWOConfig> findByWowoName(String wowoName);
}

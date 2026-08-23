package com.nexus.pms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.pms.model.entities.PmsLogs;

@Repository
public interface PmsLogsRepo extends JpaRepository<PmsLogs, Long> {

}

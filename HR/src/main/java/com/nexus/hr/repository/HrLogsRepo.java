package com.nexus.hr.repository;

import com.nexus.hr.model.entities.HrLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HrLogsRepo extends JpaRepository<HrLogs, Long> {
}

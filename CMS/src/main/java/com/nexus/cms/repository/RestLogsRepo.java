package com.nexus.cms.repository;

import com.nexus.cms.model.entities.RestLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestLogsRepo extends JpaRepository<RestLogs, Long> {
}

package com.nexus.dms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.dms.entities.DmsLogs;

@Repository
public interface DmsLogsRepo extends JpaRepository<DmsLogs, Long> {

}

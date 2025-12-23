package com.nexus.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.Logs;

@Repository
public interface LogsRepo extends JpaRepository<Logs, Long> {

}

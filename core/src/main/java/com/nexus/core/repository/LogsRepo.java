package com.nexus.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.nexus.core.model.entities.Logs;
public interface LogsRepo extends JpaRepository<Logs, Long> {

}

package com.nexus.iam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.iam.entities.Logs;

@Repository
public interface LogsRepo extends JpaRepository<Logs, Long> {

}
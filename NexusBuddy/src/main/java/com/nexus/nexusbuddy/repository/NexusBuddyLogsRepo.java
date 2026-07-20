package com.nexus.nexusbuddy.repository;

import com.nexus.nexusbuddy.model.entities.NexusBuddyLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NexusBuddyLogsRepo extends JpaRepository<NexusBuddyLogs, Long> {
}
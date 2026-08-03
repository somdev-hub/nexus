package com.nexus.nexusbuddy.repository;

import com.nexus.nexusbuddy.model.entities.NexusBuddyLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface NexusBuddyLogsRepo extends JpaRepository<NexusBuddyLogs, Long> {

    List<NexusBuddyLogs> findByClientConfigClientConfigIdInAndCreatedAtBetween(
            List<Long> clientConfigIds, Timestamp start, Timestamp end);

    List<NexusBuddyLogs> findByClientConfigClientConfigIdAndCreatedAtBetween(
            Long clientConfigId, Timestamp start, Timestamp end);
}
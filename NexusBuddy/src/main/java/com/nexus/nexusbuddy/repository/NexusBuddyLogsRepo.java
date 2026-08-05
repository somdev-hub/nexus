package com.nexus.nexusbuddy.repository;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nexus.nexusbuddy.model.entities.NexusBuddyLogs;

public interface NexusBuddyLogsRepo extends JpaRepository<NexusBuddyLogs, Long> {

    @Query("""
            SELECT n FROM NexusBuddyLogs n
            WHERE n.clientConfig.clientConfigId IN :clientConfigIds
            AND n.createdAt BETWEEN :start AND :end
                        """)
    List<NexusBuddyLogs> findByClientConfigClientConfigIdInAndCreatedAtBetween(
            List<Long> clientConfigIds, Timestamp start, Timestamp end);

    @Query("""
            SELECT n FROM NexusBuddyLogs n
            WHERE n.clientConfig.clientConfigId = :clientConfigId
            AND n.createdAt BETWEEN :start AND :end
                        """)
    List<NexusBuddyLogs> findByClientConfigClientConfigIdAndCreatedAtBetween(
            Long clientConfigId, Timestamp start, Timestamp end);
}
package com.nexus.cms.repository;

import com.nexus.cms.model.entities.KafkaBacklogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface KafkaBacklogsRepo extends JpaRepository<KafkaBacklogs, Long> {

    @Query("SELECT k FROM KafkaBacklogs k WHERE k.uuid = :uuid")
    @Transactional(readOnly = true)
    Optional<KafkaBacklogs> findByUuid(String uuid);
}

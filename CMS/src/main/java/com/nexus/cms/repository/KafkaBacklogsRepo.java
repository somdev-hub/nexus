package com.nexus.cms.repository;

import com.nexus.cms.model.entities.KafkaBacklogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KafkaBacklogsRepo extends JpaRepository<KafkaBacklogs, Long> {
}

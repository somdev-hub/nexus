package com.nexus.cms.repository;

import com.nexus.cms.model.entities.CMSLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CMSLogsRepo extends JpaRepository<CMSLogs, Long> {
}

package com.nexus.dms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexus.dms.entities.DocumentRecord;

public interface DocumentRecordRepo extends JpaRepository<DocumentRecord, Long> {

}

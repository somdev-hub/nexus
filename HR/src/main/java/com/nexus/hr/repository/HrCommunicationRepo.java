package com.nexus.hr.repository;

import com.nexus.hr.model.entities.HrCommunication;
import com.nexus.hr.model.enums.CommunicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrCommunicationRepo extends JpaRepository<HrCommunication, Long> {
    /**
     * Find all communications by sender ID
     */
    List<HrCommunication> findBySenderId(String senderId);

    /**
     * Find all communications by HR entity
     */
    List<HrCommunication> findByHrEntity_HrId(Long hrId);

    /**
     * Find all communications by status
     */
    List<HrCommunication> findByStatus(CommunicationStatus status);
}

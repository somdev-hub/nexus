package com.nexus.hr.repository;

import com.nexus.hr.model.entities.HrRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HrRequestRepo extends JpaRepository<HrRequest, Long> {
}

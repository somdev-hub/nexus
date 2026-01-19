package com.nexus.hr.repository;

import com.nexus.hr.entity.HrEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HrEntityRepo extends JpaRepository<HrEntity, Long> {
}

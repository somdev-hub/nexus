package com.nexus.hr.repository;

import com.nexus.hr.model.entities.EmployeeLeaves;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeLeavesRepo extends JpaRepository<EmployeeLeaves, Long> {
}

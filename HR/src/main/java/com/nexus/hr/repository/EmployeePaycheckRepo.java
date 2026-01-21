package com.nexus.hr.repository;

import com.nexus.hr.model.entities.EmployeePaycheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeePaycheckRepo extends JpaRepository<EmployeePaycheck, Long> {
}

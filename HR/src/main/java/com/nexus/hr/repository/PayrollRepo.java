package com.nexus.hr.repository;

import com.nexus.hr.model.entities.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollRepo extends JpaRepository<Payroll, Long> {
}
